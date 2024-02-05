package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.util.Scanner;

import java.io.IOException;

import java.io.IOException;

import java.time.LocalDate;

import java.time.LocalTime;

import java.time.format.DateTimeFormatter;

public class Chat {

  private static String horarioAtual(){
    LocalDate data = LocalDate.now();
    LocalTime horaAtual = LocalTime.now();
    DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    DateTimeFormatter formatoHoraMinutos = DateTimeFormatter.ofPattern("HH:mm");
    
    return "(" + data.format(formatoData) + " às " + horaAtual.format(formatoHoraMinutos) + ")";
    }
  
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("ec2-52-73-164-223.compute-1.amazonaws.com");
    factory.setUsername("admin");
    factory.setPassword("password");
    factory.setVirtualHost("/");   
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    String Usuario = "";
    String Remetente = "";
    
    Scanner sc = new Scanner(System.in);
    System.out.print("User: ");
    String QUEUE_Send = "";
    String [] textos = new String[3]; //Lista que será utilizada para identificar o comando e o nome posterior a ele.
    String grupo = "";
    String usuarioGrupo = "";
    String mensagem = "";
    String QUEUE_NAME = sc.nextLine();
    QUEUE_NAME = "@" + QUEUE_NAME; 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
        
        String message = new String(body, "UTF-8");
        System.out.println();
        System.out.println(message);
        System.out.print(">>>");
                        //(deliveryTag,               multiple);
        //channel.basicAck(envelope.getDeliveryTag(), false);
      }
    };
    channel.basicConsume(QUEUE_NAME, true,    consumer);
    System.out.print( QUEUE_Send + ">>>");
    String Inpt = sc.nextLine();
    while(!Inpt.equals("sair")){
      if(Inpt.substring(0,1).equals("@")){
         QUEUE_Send = Inpt;
         channel.queueDeclare(QUEUE_Send, false,   false,     false,       null);
      }
      else if(Inpt.substring(0,1).equals("#")){ //Enviar mensagens para o grupo
         QUEUE_Send = Inpt;
      }
      else if(Inpt.substring(0,1).equals("!")){ //Comando de grupos
        textos = Inpt.split(" ");
        if(textos[0].equals("!addGroup")){ //Adicionar Grupo
          grupo = textos[1];
          channel.exchangeDeclare(grupo, "fanout");
          channel.queueBind(QUEUE_NAME, grupo , "");
        }
        else if(textos[0].equals("!addUser")){ // Adicionar Usuario a um Grupo
          usuarioGrupo = '@' + textos[1];
          channel.queueDeclare(usuarioGrupo, false,   false,     false,       null);
          grupo = textos[2];
          channel.queueBind(usuarioGrupo, grupo , "");
        }
        else if(textos[0].equals("!delFromGroup")){ //Excluir Usuario de um Grupo
          usuarioGrupo = textos[1];
          grupo = textos[2];
          channel.queueUnbind(usuarioGrupo, grupo , ""); 
        }
        else if(textos[0].equals("!removeGroup")){  //Excluir um Grupo
          grupo = textos[0];
          channel.exchangeDelete(grupo);
        }
      }
      else{
        
        if(QUEUE_Send.substring(0,1).equals("@")){
          mensagem = horarioAtual() + " " + QUEUE_NAME + " diz: " + Inpt;
          channel.basicPublish("", QUEUE_Send, null,  mensagem.getBytes("UTF-8")); 
        }
        else{
          mensagem = horarioAtual() + " " + QUEUE_NAME + QUEUE_Send + " diz: " + Inpt;
          channel.basicPublish(QUEUE_Send.substring(1), QUEUE_NAME , null,  mensagem.getBytes("UTF-8")); 
        }
        
      }
      System.out.print(QUEUE_Send + " >>> ");
      Inpt = sc.nextLine();
    }
    channel.close();
    
  }
}
