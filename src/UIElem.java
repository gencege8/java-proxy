import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.awt.event.ActionEvent;

import javax.swing.*;

public class UIElem implements Runnable {
	public volatile boolean exit = false;
	public void run() {
		ProxyDaemon server=new ProxyDaemon();
		Thread t1=new Thread() {
			public void run() {
				try {
					server.runApp();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		};
		// menubar
	    JMenuBar mb;
	  
	    // JMenu
	    JMenu x;
	  
	    // Menu items
	    JMenuItem m1;
		JMenuItem m2;
		JMenuItem m3;
	  
	    // create a frame
	    JFrame f;
	    // create a frame
        f = new JFrame("ProxyServer");
  
        // create a menubar
        mb = new JMenuBar();
  
        // create a menu
        x = new JMenu("Menu");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // create menuitems
        m1 = new JMenuItem("Start");
        m2 = new JMenuItem("Stop");
        m3 = new JMenuItem("Add/remove blocked");
  
        // add menu items to menu
        x.add(m1);
        x.add(m2);
        x.add(m3);
  
        // add menu to menu bar
        mb.add(x);
  
        // add menubar to frame
        f.setJMenuBar(mb);
  
        // set the size of the frame
        f.setSize(500, 500);
        f.setVisible(true);
        
        m1.addActionListener(new ActionListener() {
        	int a=0;
        	public void actionPerformed(ActionEvent b) {
        		if(a==0){
        			t1.start();
        			a=1;
        		}
        		else {
        			server.flag=false;
        		}
        	}
        });
        m2.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent b) {
        		server.flag=true;
        	}
        });
        m3.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent b) {
        		JFrame lframe=new JFrame("Block Site");
        		JPanel lpanel=new JPanel();
        		lframe.setSize(500,100);
        		lframe.setVisible(true);
        		lframe.add(lpanel);
        		lpanel.setLayout(null);
        		JTextField userText=new  JTextField(100);
        		userText.setBounds(100,20,165,25);	
        		userText.setSize(350, 25);
        		lpanel.add(userText);
        		JButton rmv=new JButton("Remove");
        		rmv.setBounds(10, 31, 80, 25);
        		JButton add=new JButton("Add");
        		add.setBounds(10,3,80,25);
        		lpanel.add(rmv);
        		lpanel.add(add);
        		lpanel.repaint();
        		
        		add.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent b) {
        				ProxyDaemon.forbiddenAddresses.add(userText.getText());
        			}
        		});
        		rmv.addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent b) {
        				if(ProxyDaemon.forbiddenAddresses.contains(userText.getText())) {
        					ProxyDaemon.forbiddenAddresses.remove(userText.getText());
        				}
        			}
        		});
        	}
        });
        
        

	}


}
