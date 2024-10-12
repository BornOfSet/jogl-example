package jogl3.headers;

import java.awt.event.KeyEvent;

public abstract class iKeyListener implements java.awt.event.KeyListener{
	private int SHIFT = 0;
	
	@Override
	public void keyTyped(KeyEvent e) {
	}
	@Override
	public void keyPressed(KeyEvent e) {		
		if(SHIFT ==1) {
			switch(e.getKeyCode()) {
			case 32:
				OnShiftSpace();
				break;
			case 65:
				OnShiftA();
				break;
			case 68:
				OnShiftD();
				break;
			}
		}
		else {
			switch(e.getKeyCode()) {
			case 87: //VK_W
				OnPressedW();
				break;
			case 83: //VK_S
				OnPressedS();
				break;
			case 65: //VK_A
				OnPressedA();
				break;
			case 68: //VK_D
				OnPressedD();
				break;
			case 81: //VK_Q
				OnPressedQ();
				break;
			case 69: //VK_E
				OnPressedE();
				break;
			case 32:
				OnPressedSpace();
				break;
			case 86: //VK_V
				OnPressedV();
				break;
			default:
				;
			};
		}
		if(KeyEvent.VK_SHIFT == e.getKeyCode()) {
			SHIFT = 1;
		}
	}
	@Override
	public void keyReleased(KeyEvent e) {
		if(KeyEvent.VK_SHIFT == e.getKeyCode()) {
			SHIFT = 0;
		}
	}
	
	abstract public void OnPressedW();
	abstract public void OnPressedS();
	abstract public void OnPressedA();
	abstract public void OnPressedD();
	abstract public void OnPressedQ();
	abstract public void OnPressedE();
	abstract public void OnPressedSpace();
	abstract public void OnShiftSpace();
	abstract public void OnShiftA();
	abstract public void OnShiftD();
	abstract public void OnPressedV();
}
