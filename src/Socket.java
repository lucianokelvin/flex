
public class Socket {

	public void teste(){
		Socket socket = new Socket();
		socket.connect();
		
		if(socket.connected()) {
			socket.getInputStream();
		}else {
			socket.getOutputStream();
		}
		
	}
	
	
	public boolean connected() {
	return true;
	}
	
	public Socket() {
		// TODO Auto-generated constructor stub
	}
	
	public void connect() {
		
	}
	
	public void getInputStream() {
		
	}
	
	public void getOutputStream() {
		
	}
	
	public void close() {
		
	}
	
}
