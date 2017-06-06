package nio.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Server {

	private int port;
	
	public Server(int port) {
		this.port = port;
	}
	
	public void run() {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		
		ServerSocketChannel ssc = null;
		ServerSocket ss = null;
		try {
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			ss = ssc.socket();
			SocketAddress addr = new InetSocketAddress(port);
			
			ss.bind(addr);
			
			Selector selector = Selector.open();
			ssc.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("start listenning on " + port);
			
			while (true) {
				System.out.println("Blocked");
				selector.select();
				System.out.println("Resumed from blocked");
				
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				Iterator<SelectionKey> it = selectedKeys.iterator();
				while (it.hasNext()) {
					SelectionKey key = it.next();
					
					if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
						// Accept the new connection
						System.out.println("Accept a new connection");
						ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
						SocketChannel sc = serverSocketChannel.accept();
						sc.configureBlocking(false);
						
						// Register this channel to selector
						sc.register(selector, SelectionKey.OP_READ);
						
						// Remove the dealed key in order not to be dealed again
						it.remove();
					} else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
						// Read from the connection
						SocketChannel sc = (SocketChannel) key.channel();
	
						int count = 0;
						while (true) {
							buffer.clear();
							int r = sc.read(buffer);
							if (r <= 0) {
								break;
							}
							
							// Flip and echo the content
							buffer.flip();
							sc.write(buffer);
							count += r;
						}
						System.out.println("Echo " + count + "bytes from " + sc);
						it.remove();
					}
				}
			}
		} catch (IOException e) {
			System.out.println("error when listening: " + e);
		} finally {
			try {
				if (ss != null) {
					ss.close();
				}
				if (ssc != null) {
					ssc.close();
				}
			} catch (IOException e) {
				System.out.println("error when closing io: " + e);
			}
		}
	}
	
	public static void main(String[] args) {
		Server s = new Server(8080);
		s.run();
	}
}
