package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.util.Scanner;

import java.io.IOException;

import java.io.IOException;

import java.time.LocalDate;

import java.time.LocalTime;

import java.io.File;

import java.nio.file.*;

import java.io.*;

import com.google.protobuf.ByteString;

import com.google.protobuf.util.JsonFormat;

import java.time.format.DateTimeFormatter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Duration;
public class Chat {

public static String listaGrupos(String apiUrl, String username, String password, String usuario) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                  .uri(URI.create(apiUrl +"bindings"))
                  .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                  .GET()
                  .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String retorno =  response.body();
                JSONArray jsonArray = new JSONArray(retorno);
                String grupos = "";
                for (int i = 0; i < jsonArray.length(); i++) {
                  JSONObject jsonObject = jsonArray.getJSONObject(i);
                  
                  if(!jsonObject.getString("source").equals("")){
                    if(jsonObject.getString("destination").equals(usuario)){
                      if(i != jsonArray.length() - 1){
                        grupos += jsonObject.getString("source") + ", ";
                      }
                      else{
                        grupos += jsonObject.getString("source");
                      }
                    }
                  }
                }
                return grupos;
            } else {
                return "Erro ao fazer a requisição: " + response.statusCode() + " - " + response.body();
            }
        } catch (Exception e) {
            return "Erro ao enviar a requisição: " + e.getMessage();
        }
    }

  public static String listaUsuariosGrupos(String apiUrl, String username, String password, String group) {
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        
        HttpRequest request = HttpRequest.newBuilder()
                  .uri(URI.create(apiUrl + "exchanges/%2F/"+ group + "/bindings/source"))
                  .header("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString((username + ":" + password).getBytes()))
                  .GET()
                  .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String retorno =  response.body();
                JSONArray jsonArray = new JSONArray(retorno);
                String destination = "";
                for (int i = 0; i < jsonArray.length(); i++) {
                  JSONObject jsonObject = jsonArray.getJSONObject(i);
                  
                  if(i == 0){
                    destination = jsonObject.getString("destination");
                  }
                  else{
                    destination += " " + jsonObject.getString("destination");
                  }
                }
                return destination;
            } else {
                return "Erro ao fazer a requisição: " + response.statusCode() + " - " + response.body();
            }
        } catch (Exception e) {
            return "Erro ao enviar a requisição: " + e.getMessage();
        }
    }
  
  private static String horarioAtual(){
    LocalTime horaAtual = LocalTime.now();
    DateTimeFormatter formatoHoraMinutos = DateTimeFormatter.ofPattern("HH:mm");
    
    return horaAtual.format(formatoHoraMinutos);
  }
  private static String dataAtual(){
    LocalDate data = LocalDate.now();
    DateTimeFormatter formatoData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    return data.format(formatoData);
  }
  
  private static Void DecideEnvio(String QUEUE_Send, Mensagem.MensagemUsuario.Builder builderMensagem, Channel channel, String QUEUE_NAME){
    try {
      if(QUEUE_Send.substring(0,1).equals("@")){
          
          builderMensagem.setGrupo("");
          Mensagem.MensagemUsuario mensagemUsuario = builderMensagem.build();
          byte[] buffer = mensagemUsuario.toByteArray();
          channel.basicPublish("", QUEUE_Send, null,  buffer); 
        }
      else{
        
        builderMensagem.setGrupo(QUEUE_Send);
        Mensagem.MensagemUsuario mensagemUsuario = builderMensagem.build();
        byte[] buffer = mensagemUsuario.toByteArray();
        channel.basicPublish(QUEUE_Send.substring(1), QUEUE_NAME , null, buffer); 
      }
    }
    catch(IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    //factory.setHost("ec2-3-210-53-94.compute-1.amazonaws.com");
    factory.setHost("Teste-ed4bec25de79f707.elb.us-east-1.amazonaws.com");
    factory.setUsername("admin");
    factory.setPassword("password");
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    final Channel channel2 = connection.createChannel();
    String Usuario = "";
    String Remetente = "";
    String usuarioRabbit = "admin";
    String senhaRabbit = "password";
    String apiUrl = "http://SDHTTP-1846640378.us-east-1.elb.amazonaws.com:80/api/";
    Scanner sc = new Scanner(System.in);
    System.out.print("User: ");
    String QUEUE_Send = "";
    String [] textos = new String[3]; //Lista que será utilizada para identificar o comando e o nome posterior a ele.
    String grupo = "";
    String usuarioGrupo = "";
    String mensagem = "";
    String QUEUE_NAME = sc.nextLine();
    final String nomeUsuario = QUEUE_NAME;
    QUEUE_NAME = "@" + QUEUE_NAME; 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    channel2.queueDeclare(QUEUE_NAME+"Up", false,   false,     false,       null);
    Consumer consumer = new DefaultConsumer(channel) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
        
        //String message = new String(body, "UTF-8");
        byte[] message = body;
        Mensagem.MensagemUsuario mensagemUsuario = Mensagem.MensagemUsuario.parseFrom(message);
        Mensagem.Conteudo conteudoMensagem = mensagemUsuario.getConteudo();
        System.out.println();
        if(!mensagemUsuario.getEmissor().equals("@" + nomeUsuario)){
          System.out.println(mensagemUsuario.getData() + " às " + mensagemUsuario.getHora() + " " + mensagemUsuario.getGrupo() + mensagemUsuario.getEmissor() + " diz: " + conteudoMensagem.getCorpo().toString("UTF-8"));
        }
        System.out.print(">>>");
                        //(deliveryTag,               multiple);
        //channel.basicAck(envelope.getDeliveryTag(), false);
      }
    };
    Consumer consumer2 = new DefaultConsumer(channel2) {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)           throws IOException {
        
        //String message = new String(body, "UTF-8");
        byte[] message = body;
        Mensagem.MensagemUsuario mensagemUsuario = Mensagem.MensagemUsuario.parseFrom(message);
        Mensagem.Conteudo conteudoMensagem = mensagemUsuario.getConteudo();
        try {
          File file = new File("/home/ubuntu/environment/chat/uploads/"+conteudoMensagem.getNome());
          if (file.createNewFile()) {
              System.out.println("Arquivo criado com sucesso!");
          } else {
              System.out.println("O arquivo já existe.");
          }
          ByteString conteudo = conteudoMensagem.getCorpo();
          byte[] bytes = conteudo.toByteArray();
          ByteString byteString = ByteString.copyFrom(bytes);
          FileOutputStream outputStream = new FileOutputStream(file, true);
          outputStream.write(byteString.toByteArray());
          outputStream.close();
          } catch (IOException e) {
            System.err.println("Erro ao criar o arquivo: " + e.getMessage());
        }
        System.out.println();
        System.out.println(mensagemUsuario.getData() + " às " + mensagemUsuario.getHora() + " Arquivo " + conteudoMensagem.getNome() + " recebido de " + mensagemUsuario.getEmissor() + "!");
        System.out.print(">>>");
                        //(deliveryTag,               multiple);
        //channel.basicAck(envelope.getDeliveryTag(), false);
      }
    };
    channel.basicConsume(QUEUE_NAME, true, consumer);
    channel2.basicConsume(QUEUE_NAME + "Up", true, consumer2);
    System.out.print( QUEUE_Send + " >>> ");
    String Inpt = sc.nextLine();
    while(!Inpt.equals("sair")){
      if(Inpt.substring(0,1).equals("@")){
         QUEUE_Send = Inpt;
         channel.queueDeclare(QUEUE_Send, false,   false,     false,       null);
      }
      else if(Inpt.substring(0,1).equals("#")){ //Enviar mensagens para o grupo
         QUEUE_Send = Inpt;
      }
      else if(Inpt.substring(0,1).equals("!")){
        textos = Inpt.split(" ");
        if(textos[0].equals("!upload")){
          final String[] textosFinal = textos;
          final String QueueNameAux = QUEUE_NAME;
          final String QueueSendAux = QUEUE_Send;
          Thread uploadThread = new Thread(() -> {
          try {
              Mensagem.MensagemUsuario.Builder builderMensagem = Mensagem.MensagemUsuario.newBuilder();
          
              Mensagem.Conteudo.Builder builderConteudo = Mensagem.Conteudo.newBuilder();
              final Path source = Paths.get(textosFinal[1]);
              String tipoMime = Files.probeContentType(source);
              String[] textosSeparados = textosFinal[1].split("/");
              builderConteudo.setNome(textosSeparados[textosSeparados.length - 1]);
              builderConteudo.setCorpo(ByteString.readFrom(new FileInputStream(new File("/home/ubuntu/environment"+textosFinal[1]))));
              builderConteudo.setTipo(tipoMime);
              builderMensagem.setHora(horarioAtual());
              builderMensagem.setData(dataAtual());
              builderMensagem.setEmissor(QueueNameAux);
              builderMensagem.setConteudo(builderConteudo);
              System.out.println("\nEnviando " + textosFinal[1] + " para "  + QueueSendAux);
              DecideEnvio(QueueSendAux+"Up", builderMensagem, channel2, QueueNameAux); // Usar o novo canal de upload
              System.out.println("Arquivo " + textosFinal[1] + " foi enviado para "  + QueueSendAux);
              System.out.print(QueueSendAux + " >>> ");
          } catch (IOException e) {
              e.printStackTrace();
          }
          });
          uploadThread.start();
          
          }
        else if(textos[0].equals("!addGroup")){ //Adicionar Grupo
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
          grupo = textos[1];
          channel.exchangeDelete(grupo);
        }
        else if(textos[0].equals("!listUsers")){  //Listar Usuários do Grupo
          grupo = textos[1];
          String response = listaUsuariosGrupos(apiUrl, usuarioRabbit, senhaRabbit, grupo);
          System.out.println(response);
        }
        else if(textos[0].equals("!listGroups")){  //Listar Usuários do Grupo
          String response = listaGrupos(apiUrl, usuarioRabbit, senhaRabbit, QUEUE_NAME);
          System.out.println(response);
        } 
      }
      else{
        Mensagem.MensagemUsuario.Builder builderMensagem = Mensagem.MensagemUsuario.newBuilder();
        
        Mensagem.Conteudo.Builder builderConteudo = Mensagem.Conteudo.newBuilder();
        builderConteudo.setCorpo(ByteString.copyFrom(Inpt,"UTF-8"));
        
        builderMensagem.setHora(horarioAtual());
        builderMensagem.setData(dataAtual());
        builderMensagem.setEmissor(QUEUE_NAME);
        builderMensagem.setConteudo(builderConteudo);
        DecideEnvio(QUEUE_Send, builderMensagem, channel, QUEUE_NAME);
        // if(QUEUE_Send.substring(0,1).equals("@")){
          
        //   builderMensagem.setGrupo("");
          
        //   channel.basicPublish("", QUEUE_Send, null,  buffer); 
        // }
        // else{
          
        //   builderMensagem.setGrupo(QUEUE_Send);
        //   channel.basicPublish(QUEUE_Send.substring(1), QUEUE_NAME , null, buffer); 
        // }
        
      }
      System.out.print(QUEUE_Send + " >>> ");
      Inpt = sc.nextLine();
    }
    channel.close();
    
  }
}
