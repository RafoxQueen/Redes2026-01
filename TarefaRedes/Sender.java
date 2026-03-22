package TarefaRedes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
//import java.util.concurrent.*;

public class Sender {
	
	static int TIMEOUT = 10000;

    static int idAtual = 0;
    
    // buffer de pacotes enviados aguardando ACK
    static Map<Integer, SegmentoConfiavel> bufferEnvio = new HashMap<>();

    // armazena instante de envio para controle de timeout
    static Map<Integer, Long> tempoEnvio = new HashMap<>();
    
    static SegmentoConfiavel pacoteAtrasado = null;
    
    static DatagramSocket clientSocket;
    
    static String ip;
    
    static int porta;
    
    public static void main(String[] args) throws Exception {
    	
    	Scanner sc = new Scanner(System.in);

        System.out.print("IP do receiver (default 127.0.0.1): ");
        ip = sc.nextLine();
        if(ip.isEmpty()) ip="127.0.0.1";

        System.out.print("Porta do receiver: ");
        porta = sc.nextInt();
        sc.nextLine();

        clientSocket = new DatagramSocket();
		
        new Thread(new EscutaACK()).start();
        new Thread(new ControleTimeout()).start();
        
        while (true) {
        	System.out.print("Mensagem: ");
            String msg = sc.nextLine();
            
            System.out.println("1-normal\n2-perda\n3-fora de ordem\n4-duplicada\n5-lenta");
            int opcao = sc.nextInt();
            sc.nextLine();
            
            String metodo = "";
            
            if(opcao==1) metodo = "normal";
            if(opcao==2) metodo = "perda";
            if(opcao==3) metodo = "fora de ordem";
            if(opcao==4) metodo = "duplicada";
            if(opcao==5) metodo = "lenta";
            
            idAtual++;
            
            System.out.println("Mensagem \""+msg+"\" enviada como ["+metodo+"] com id "+ idAtual);
            
            SegmentoConfiavel seg = new SegmentoConfiavel(idAtual,msg,false);
            
            bufferEnvio.put(idAtual,seg);
            tempoEnvio.put(idAtual,System.currentTimeMillis());
            
            enviarMensagem(seg,opcao);
            
        }
	}
    
    public static void enviarMensagem(SegmentoConfiavel seg,int op) throws Exception{

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);

        out.writeObject(seg);
        byte[] data = bos.toByteArray();

        DatagramPacket packet =
                new DatagramPacket(data,data.length,
                        InetAddress.getByName(ip),porta);

        if(op==1){ //Normal
        	clientSocket.send(packet);
        }
        
        if(op==2){ //Perda
        }
        
        if(op==3) {
        	pacoteAtrasado = seg;
        }

        if(op==4){ //Duplicada
        	clientSocket.send(packet);
        	clientSocket.send(packet);
        }

        if(op==5){//Lenta
            Thread.sleep(3000);
            clientSocket.send(packet);
        }
        
        if(pacoteAtrasado != null & op != 3) {
        	SegmentoConfiavel seg2 = pacoteAtrasado;
        	pacoteAtrasado = null;
        	enviarMensagem(seg2,1);
        }
        
    }
    
    static class EscutaACK implements Runnable{

        public void run(){

            try{

                byte[] buffer = new byte[1024];

                while(true){

                    DatagramPacket packet =
                            new DatagramPacket(buffer,buffer.length);

                    clientSocket.receive(packet);

                    ObjectInputStream in =
                            new ObjectInputStream(
                                    new ByteArrayInputStream(packet.getData()));

                    SegmentoConfiavel ack =
                            (SegmentoConfiavel) in.readObject();

                    if(ack.ack){

                        System.out.println(
                                "Mensagem id "+ack.id+
                                " recebida pelo receiver.");

                        bufferEnvio.remove(ack.id);
                        tempoEnvio.remove(ack.id);
                    }
                }

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    // THREAD QUE CONTROLA TIMEOUT

    static class ControleTimeout implements Runnable{

        public void run(){

            while(true){

                try{

                    verificarTimeout();

                    Thread.sleep(100);

                }catch(Exception e){}
            }
        }
    }

    static void verificarTimeout() throws Exception{

        long agora = System.currentTimeMillis();

        for(Integer id : new ArrayList<>(tempoEnvio.keySet())){

            long tempo = tempoEnvio.get(id);
            
            if(agora - tempo > TIMEOUT){

                System.out.println(
                        "Mensagem id "+id+
                        " deu timeout, reenviando.");

                SegmentoConfiavel seg =
                        bufferEnvio.get(id);

                if(seg!=null){

                	enviarMensagem(seg,1);

                    tempoEnvio.put(id,System.currentTimeMillis());
                }
            }
        }
    }
}
	
