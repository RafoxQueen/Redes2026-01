package TarefaRedes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.*;
import java.util.*;
import java.net.*;

public class Receiver {
	static DatagramSocket socket;

    // buffer para pacotes fora de ordem
    static Map<Integer, SegmentoConfiavel> bufferRecebimento = new HashMap<>();

    // ids já recebidos
    static Set<Integer> recebidos = new HashSet<>();

    static int esperado = 1;

    public static void main(String[] args) throws Exception{
    	
    	URL url = new URL("http://checkip.amazonaws.com");
        Scanner scURL = new Scanner(url.openStream());

        String ip = scURL.nextLine();
        System.out.println("IP público: " + ip);

        scURL.close();
    	
        Scanner sc = new Scanner(System.in);

        System.out.print("Porta do receiver: ");
        int porta = sc.nextInt();

        socket = new DatagramSocket(porta);

        byte[] buffer = new byte[1024];

        while(true){

            DatagramPacket packet =
                    new DatagramPacket(buffer,buffer.length);

            socket.receive(packet);

            ObjectInputStream in =
                    new ObjectInputStream(
                            new ByteArrayInputStream(packet.getData()));

            SegmentoConfiavel seg =
                    (SegmentoConfiavel) in.readObject();

            int id = seg.id;

            if(recebidos.contains(id)){

                System.out.println("Mensagem id "+id+" recebida de forma duplicada");

            }
            else{

                recebidos.add(id);

                if(id==esperado){

                    System.out.println("Mensagem id "+id+" recebida na ordem, entregando para a camada de aplicação.");

                    esperado++;

                    while(bufferRecebimento.containsKey(esperado)){

                        System.out.println("Mensagem id "+esperado+" saindo da espera, entregando para a camada de aplicação.");

                        bufferRecebimento.remove(esperado);
                        esperado++;
                    }

                }
                else{

                    bufferRecebimento.put(id,seg);

                    List<Integer> faltando = new ArrayList<>();

                    for(int i=esperado;i<id;i++)
                        if(!recebidos.contains(i))
                            faltando.add(i);

                    System.out.println("Mensagem id "+id+" recebida fora de ordem, ainda não recebidos os identificadores "+faltando);
                }
            }

            enviarACK(id,packet);
        }
    }

    static void enviarACK(int id,DatagramPacket packet) throws Exception{

        SegmentoConfiavel ack =
                new SegmentoConfiavel(id,"ACK",true);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);

        out.writeObject(ack);
        byte[] data = bos.toByteArray();

        DatagramPacket resp =
                new DatagramPacket(data,data.length,
                        packet.getAddress(),
                        packet.getPort());

        socket.send(resp);
    }
}
