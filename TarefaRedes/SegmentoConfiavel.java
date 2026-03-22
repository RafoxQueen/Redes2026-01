package TarefaRedes;

import java.io.Serializable;

public class SegmentoConfiavel implements Serializable {
	private static final long serialVersionUID = 1L;

	public int id;
	
	public String mensagem;
	
	public boolean ack;
	
	public SegmentoConfiavel(int id, String mensagem, boolean ack) {
		this.id = id;
		this.mensagem = mensagem;
		this.ack = ack;
	}
}
