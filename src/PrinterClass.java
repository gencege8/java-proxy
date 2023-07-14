import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class PrinterClass {

	static Queue<Queue<String>> handlerList = new LinkedBlockingQueue<Queue<String>>();
	
	final static PrinterHelper pH = new PrinterHelper();

	Queue<String> list;

	static Object lock = new Object();

	static Object removeLock = new Object();

	public PrinterClass() {
		list = new LinkedBlockingQueue<>();
		handlerList.add(list);
		synchronized (lock) {
			lock.notify();
		}
	}

	public void add(String str) {
		list.add(str);
		synchronized (lock) {
			lock.notify();
		}
	}

	public void removeThread() {
		while (!list.isEmpty()) {
			synchronized (removeLock) {
				try {
					removeLock.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		handlerList.remove(list);
	}

	static class PrinterHelper extends Thread {
		
		PrinterHelper() {
			start();
		}

		@Override
		public void run() {

			while (true) {
				synchronized (lock) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				Queue<String> tmp = handlerList.element();
				while (!tmp.isEmpty()) {
					System.out.println(tmp.remove());
				}
				synchronized (removeLock) {
					removeLock.notifyAll();
				}

			}

		}

	}

}
