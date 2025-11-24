package main;
import java.awt.event.KeyListener;

import javax.swing.event.MenuKeyEvent;

import java.awt.event.KeyEvent;
public class keyHandler implements KeyListener{
public boolean upPressed,downPressed,leftPressed,rightPressed;
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		int code = e.getKeyCode();
		if(code==MenuKeyEvent.VK_Z) {
			upPressed=true;
		}
if(code==MenuKeyEvent.VK_S) {
	downPressed =true;
		}
if(code==MenuKeyEvent.VK_Q) {
	leftPressed=true;
}
if(code==MenuKeyEvent.VK_D) {
	rightPressed=true;
}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		int code = e.getKeyCode();
		if(code==MenuKeyEvent.VK_Z) {
			upPressed=false;
		}
if(code==MenuKeyEvent.VK_S) {
	downPressed =false;
		}
if(code==MenuKeyEvent.VK_Q) {
	leftPressed=false;
}
if(code==MenuKeyEvent.VK_D) {
	rightPressed=false;
}
	}

}
