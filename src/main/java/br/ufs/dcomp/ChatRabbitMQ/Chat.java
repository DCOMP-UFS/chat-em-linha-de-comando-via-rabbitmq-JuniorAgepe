package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

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
    factory.setHost("ec2-54-146-179-243.compute-1.amazonaws.com"); // Alterar
    factory.setUsername("admin"); // Alterar
    factory.setPassword("password"); // Alterar
    factory.setVirtualHost("/");   
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    Channel channelEnvia = connection.createChannel();
    String Usuario = "";
    String Remetente = "";
                      //(queue-name, durable, exclusive, auto-delete, params); 
    
     Scanner sc = new Scanner(System.in);
     System.out.print("User: ");
     String QUEUE_Send = "";
     String QUEUE_NAME = sc.nextLine();
     QUEUE_NAME = "@" + QUEUE_NAME; 
     channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
        
        String message = new String(body, "UTF-8");
        System.out.println();
        System.out.println(horarioAtual() + "Usuario " + " diz: " + message);
        System.out.print(">>>");
                        //(deliveryTag,               multiple);
        //channel.basicAck(envelope.getDeliveryTag(), false);
      }
    };
    channel.basicConsume(QUEUE_NAME, true,    consumer);
    System.out.print(">>>");
    String Inpt = sc.nextLine();
    while(!Inpt.equals("sair")){
      if(Inpt.substring(0,1).equals("@")){
         QUEUE_Send = Inpt;
         channelEnvia.queueDeclare(QUEUE_Send, false,   false,     false,       null);
      }
      else{
        channelEnvia.basicPublish("", QUEUE_Send, null,  Inpt.getBytes("UTF-8"));
      }
      System.out.print(QUEUE_Send + " >>> ");
      Inpt = sc.nextLine();
    }
    channelEnvia.close();
    
  }
}
