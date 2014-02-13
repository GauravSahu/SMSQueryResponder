/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package smsqueryresponder;

/**
 *
 * @author QWERTY
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        {
		ReadMessages app = new ReadMessages();
		try
		{
			app.doIt(null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
    }

}
