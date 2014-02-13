// SMSLib for Java v3
// A Java API library for sending and receiving SMS via a GSM modem
// or other supported gateways.
// Web Site: http://www.smslib.org
//
// Copyright (C) 2002-2012, Thanasis Delenikas, Athens/GREECE.
// SMSLib is distributed under the terms of the Apache License version 2.0
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.smslib;




import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ajwcc.pduUtils.gsm3040.PduUtils;
import org.ajwcc.pduUtils.gsm3040.SmsDeliveryPdu;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.String;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import smsqueryresponder.MySqlFrame;
import smsqueryresponder.setting;

/**
 * Class representing an inbound sms message.
 */
public class InboundMessage extends Message
{
	private static final long serialVersionUID = 2L;
        public final String USER_AGENT = "Chrome";
        public  String sender;
        public  String smstext;
        public  String  httpaddres;
        public  String pretext;
     




	/**
	 * Enumeration representing various message classes.
	 */
	public enum MessageClasses
	{
		/**
		 * Already-read messages.
		 */
		READ,
		/**
		 * Not-read messages.
		 */
		UNREAD,
		/**
		 * All messages, whether read or unread.
		 */
		ALL
	}
       

	private String originator;

	private int memIndex;

	private String memLocation;

	private int mpRefNo;

	private int mpMaxNo;

        private String httpaddress;

       // private boolean flage;

	private int mpSeqNo;

	private String mpMemIndex;

	protected String ud;

	protected String udh;

	protected String smscNumber;

	private boolean endsWithMultiChar;

        public void sethttpadd(String httpaddres){
           
            httpaddres =  setting.httpadd;
        }

        public String gethttpadd(){

            return httpaddres;
        }

        public void setpredefinetext(String pretext){
            setting obj = new setting();
           pretext = obj.predefinetext;
        }

        public String getpredefinetext(){

            return pretext;
        }

        public void setSmstext(String smstext1){
           smstext = smstext1 ;
        }
        public void setSenderno(String senderno){
           sender = senderno;
        }
        public String getSmstext(){
           return smstext;
        }
        public String getSenderno(){
           return sender;
        }

	public InboundMessage(Date date, String myOriginator, String text, int myMemIndex, String myMemLocation)
	{
		super(MessageTypes.INBOUND, date, text);
		setOriginator(myOriginator);
		setMemIndex(myMemIndex);
		setMemLocation(myMemLocation);
		setMpRefNo(0);
		setMpMaxNo(0);
		setMpSeqNo(0);
		setMpMemIndex(-1);
		setSmscNumber(null);
               // setFlage(true);
	}

	public InboundMessage(MessageTypes type, int myMemIndex, String myMemLocation)
	{
		super(type, null, null);
		setOriginator("");
		setMemIndex(myMemIndex);
		setMemLocation(myMemLocation);
		setMpRefNo(0);
		setMpMaxNo(0);
		setMpSeqNo(0);
		setMpMemIndex(-1);
		setSmscNumber(null);
	}

	public InboundMessage(SmsDeliveryPdu pdu, int myMemIndex, String myMemLocation)
	{
		super(MessageTypes.INBOUND, null, null);
		setMemIndex(myMemIndex);
		setMemLocation(myMemLocation);
		setMpRefNo(0);
		setMpMaxNo(0);
		setMpSeqNo(0);
		setMpMemIndex(-1);
		int dcsEncoding = PduUtils.extractDcsEncoding(pdu.getDataCodingScheme());
		switch (dcsEncoding)
		{
			case PduUtils.DCS_ENCODING_7BIT:
				setEncoding(MessageEncodings.ENC7BIT);
				break;
			case PduUtils.DCS_ENCODING_8BIT:
				setEncoding(MessageEncodings.ENC8BIT);
				break;
			case PduUtils.DCS_ENCODING_UCS2:
				setEncoding(MessageEncodings.ENCUCS2);
				break;
			default:
				throw new RuntimeException("Unknown encoding value: " + dcsEncoding);
		}
		if ((pdu.getAddressType() & PduUtils.ADDRESS_TYPE_ALPHANUMERIC) == PduUtils.ADDRESS_TYPE_ALPHANUMERIC) this.setOriginator(pdu.getAddress());
		else if ((pdu.getAddressType() & PduUtils.ADDRESS_TYPE_INTERNATIONAL) == PduUtils.ADDRESS_TYPE_INTERNATIONAL) this.setOriginator("+" + pdu.getAddress());
		else this.setOriginator(pdu.getAddress());
		this.setDate(pdu.getTimestamp());
		this.setSmscNumber(pdu.getSmscAddress());
		extractData(pdu);
		if (pdu.isConcatMessage())
		{
			this.setMpRefNo(pdu.getMpRefNo());
			this.setMpMaxNo(pdu.getMpMaxNo());
			this.setMpSeqNo(pdu.getMpSeqNo());
		}
		if (pdu.isPortedMessage())
		{
			this.setDstPort(pdu.getDestPort());
			this.setSrcPort(pdu.getSrcPort());
		}
		if (pdu.hasTpUdhi())
		{
			this.udh = PduUtils.bytesToPdu(pdu.getUDHData());
		}
		this.ud = PduUtils.bytesToPdu(pdu.getUserDataAsBytes());
		// NEW
		if (getEncoding() == MessageEncodings.ENC7BIT)
		{
			// check if the last byte of the udData is 1b
			// but the septets need to be expanded
			byte[] temp = PduUtils.encodedSeptetsToUnencodedSeptets(pdu.getUDData());
			if (temp.length == 0)
			{
				this.endsWithMultiChar = false;
			}
			else if (temp[temp.length - 1] == 0x1b)
			{
				this.endsWithMultiChar = true;
			}
		}
	}



	public void setEndsWithMultiChar(boolean b)
	{
		this.endsWithMultiChar = b;
	}

	public boolean getEndsWithMultiChar()
	{
		return this.endsWithMultiChar;
	}

	/**
	 * Returns the originator of this message.
	 * 
	 * @return The originator of this message.
	 */
	public String getOriginator()
	{
		return this.originator;
	}

	void setOriginator(String myOriginator)
	{
		this.originator = myOriginator;
	}

	/**
	 * Returns the GSM Modem/Phone memory index from which this message was
	 * read.
	 * 
	 * @return The memory index.
	 * @see #getMemLocation()
	 */
	public int getMemIndex()
	{
		return this.memIndex;
	}

	public void setMemIndex(int myMemIndex)
	{
		this.memIndex = myMemIndex;
	}

	/**
	 * Returns the GSM Modem/Phone memory location from which this message was
	 * read.
	 * 
	 * @return The memory location identifier.
	 * @see #getMemIndex()
	 */
	public String getMemLocation()
	{
		return this.memLocation;
	}

	public void setMemLocation(String myMemLocation)
	{
		this.memLocation = myMemLocation;
	}

	public int getMpMaxNo()
	{
		return this.mpMaxNo;
	}

	public void setMpMaxNo(int myMpMaxNo)
	{
		this.mpMaxNo = myMpMaxNo;
	}

	public String getMpMemIndex()
	{
		return this.mpMemIndex;
	}

	public void setMpMemIndex(int myMpMemIndex)
	{
		if (myMpMemIndex == -1) this.mpMemIndex = "";
		else this.mpMemIndex += (this.mpMemIndex.length() == 0 ? "" : ",") + myMpMemIndex;
	}

	public int getMpRefNo()
	{
		return this.mpRefNo;
	}

	public void setMpRefNo(int myMpRefNo)
	{
		this.mpRefNo = myMpRefNo;
	}

    /*    public void setFlage(boolean gaurav){

                this.flage = gaurav;
        }

        public boolean getFlage(){
                return this.flage;
        }*/

	public int getMpSeqNo()
	{
		return this.mpSeqNo;
	}

	public void setMpSeqNo(int myMpSeqNo)
	{
		this.mpSeqNo = myMpSeqNo;
	}
         public void savetodatabase() throws ClassNotFoundException, SQLException{
       MySqlFrame obj1 = new MySqlFrame();
       DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
       String db_sender = getSenderno();
       String db_smstext = getSmstext();
       Date date = new Date();
       String host = obj1.db_server;
       String port  = obj1.db_port;
       String username = obj1.db_username;
       String password = obj1.db_password;
       String db_name = obj1.db_name;
       String db_table = obj1.db_table;

       try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection con = DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+db_name+"", username, password);
            PreparedStatement pstmt1 = con.prepareStatement("CREATE TABLE "+db_table+" (SENDER VARCHAR(12),SMSTEXT CHAR(500),DATE varchar(50)) ");
            PreparedStatement pstmt2 =con.prepareStatement("insert into "+db_table+"VALUES ("+db_sender+", "+db_smstext+","+dateFormat.format(date)+"");
          //  pstmt.setString(1,test[1]);
           // pstmt.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(setting.class.getName()).log(Level.SEVERE, null, ex);
        }

   }


            public String gethttpvar() throws Exception{

                String url = "http://localhost:8084/smsenablerweb.com/sms";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		//add request header
		con.setRequestProperty("User-Agent", USER_AGENT);


                String a = getSenderno();
                String b = getSmstext();
                String urlParameters = "smst=+"+b+"&smsn="+a+"";
                con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		//int responseCode = con.getResponseCode();
		//System.out.println("\nSending 'GET' request to URL : " + url);
		//System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		//print result
		return(response.toString());

        }


	@Override
	public String toString()
	{       
                String asdf = getText();
                String qwerty = this.originator;
                setSmstext(asdf);
                setSenderno(qwerty);
               
               // setSenderno(((StatusReportMessage) this).getRecipient());
		String str = "\n";
		str += "===============================================================================";
		str += "\n";
		str += "<< " + getClass().getSimpleName() + " >>";
		str += "\n";
		str += "-------------------------------------------------------------------------------";
		str += "\n";
		str += " Gateway Id: " + getGatewayId();
		str += "\n";
		str += " Message UUID: " + getUuid();
		str += "\n";
		str += " Encoding: " + (getEncoding() == MessageEncodings.ENC7BIT ? "7-bit" : (getEncoding() == MessageEncodings.ENC8BIT ? "8-bit" : "UCS2 (Unicode)"));
		str += "\n";
		str += " Date: " + getDate();
		str += "\n";
		str += " Dispatched via SMSC: " + getSmscNumber();
		str += "\n";
		if (this instanceof InboundBinaryMessage)
		{
			InboundBinaryMessage binaryMessage = (InboundBinaryMessage) this;
			if (binaryMessage.getDataBytes() != null)
			{
				String binaryString = PduUtils.bytesToPdu((binaryMessage).getDataBytes());
				str += " Binary: " + binaryString;
				str += "\n";
			}
			else
			{
				str += " Binary: null";
				str += "\n";
			}
			if (this instanceof InboundEncryptedMessage)
			{
				try
				{
					InboundEncryptedMessage encryptedMessage = (InboundEncryptedMessage) this;
					str += " Message is **encrypted**, decrypted text: " + encryptedMessage.getDecryptedText() + "\n";
				}
				catch (Exception e)
				{
					str += " Could not decrypt message: " + e.getMessage() + "\n";
				}
			}
		}
		else
		{
			str += " Text: " + getText();
			str += "\n";
                        
			try
			{
                                
                                str += " Http: " + (String)gethttpvar();
                                str += "\n";
				str += " PDU data: " + getPduUserData();
				str += "\n";
			}
			catch (Exception e)
			{
				str += " PDU data: <cannot extract properly, udh present>";
				str += "\n";
			}
		}
		str += " Originator: " + this.originator;
		str += "\n";
		if (this instanceof StatusReportMessage)
		{
			str += " Original Recipient: " + ((StatusReportMessage) this).getRecipient();
			str += "\n";
			str += " Delivery Status: " + ((StatusReportMessage) this).getStatus();
			str += "\n";
			str += " SMSC Ref No: " + ((StatusReportMessage) this).getRefNo();
			str += "\n";
			str += " Sent Date: " + ((StatusReportMessage) this).getSent();
			str += "\n";
			str += " Delivered Date: " + ((StatusReportMessage) this).getReceived();
			str += "\n";
		}
		str += " Memory Index: " + getMemIndex();
		str += "\n";
		str += " Multi-part Memory Index: " + getMpMemIndex();
		str += "\n";
		str += " Memory Location: " + getMemLocation();
		str += "\n";
		str += " Source / Destination Ports: " + getSrcPort() + " / " + getDstPort();
		str += "\n";
		str += "===============================================================================";
		str += "\n";

             try {
                    Service.getInstance().deleteMessage(this);
                } catch (TimeoutException ex) {
                Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GatewayException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
               

            String sentsms="";
        try {
             sentsms = gethttpvar();
        } catch (Exception ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
             String sendernumber = getSenderno();
               /* String result = "";
               
                StringTokenizer st = new StringTokenizer(sentsms);
                while (st.hasMoreTokens()) {
                    String w = st.nextToken();
                    if (w.equals("<br>")) {
                            result = result + "\r\n";
                    } else {
                         result = result + w;
                }
                }*/
             String replace = sentsms.replace("<br>", "\r\n");
             if(pretext.equals(null))
             {
             // OutboundMessage msg = new OutboundMessage("+918090779037", "hello");
              OutboundMessage msg = new OutboundMessage(sendernumber, replace);
        try {
            Service.getInstance().sendMessage(msg);
        } catch (TimeoutException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GatewayException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
            }
 else{
             OutboundMessage msg = new OutboundMessage(sendernumber,pretext);
             try {
            Service.getInstance().sendMessage(msg);
        } catch (TimeoutException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GatewayException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(InboundMessage.class.getName()).log(Level.SEVERE, null, ex);
        }
        }

	return str;
                
	}

        


	protected void extractData(SmsDeliveryPdu pdu)
	{
		// binary messages belong in the InboundBinaryMessage subclass not here
		if (pdu.isBinary()) throw new RuntimeException("Trying to apply a binary pdu to an InboundMessage");
		this.setText(pdu.getDecodedText());
	}

	@Override
	public String getPduUserData()
	{
		if ((this.udh != null) && (getEncoding() == MessageEncodings.ENC7BIT)) { throw new RuntimeException("getPduUserData() not supported for 7-bit messages with UDH"); }
		return this.ud;
	}

	@Override
	public String getPduUserDataHeader()
	{
		// NOTE: when for a concat message
		// this will only return the UDH of the FIRST part of the
		// multi-part message since subsequent parts are added
		// via addText() or addDataBytes()
		return this.udh;
	}

	@Override
	public void addText(String s) throws UnsupportedEncodingException
	{
		super.addText(s);
		// NOTE: adjust stored ud data
		// only for UCS2 since multi-part 7-bit is not supported 
		// for the getPduUserData() and 8-bit encodings are handled 
		// in a different class
		if (getEncoding() == MessageEncodings.ENCUCS2)
		{
			this.ud = this.ud + PduUtils.bytesToPdu(s.getBytes("UTF-16BE"));
		}
	}

	public String getSmscNumber()
	{
		return this.smscNumber;
	}

	private void setSmscNumber(String mySmscNumber)
	{
		this.smscNumber = mySmscNumber;
	}

        
}
