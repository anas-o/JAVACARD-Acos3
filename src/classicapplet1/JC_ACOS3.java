/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package classicapplet1;

import javacard.framework.*;

/**
 *
 * @author samie
 */
public class JC_ACOS3 extends Applet {
   
    public static byte PersFile     = 0x00;
    public static int N_OF_FILES;
    public static byte[] SecFile    = { 0x0A, 0x0B, 0x0C, 0x0D, 0x01, 0x02, 0x03, 0x04 };
    public static byte[] ManagFile = new byte [64*6]; // le maximum des fichiers qu'on peut créer est 64
    public static byte[] UserFile = new byte [255]; //le maximum des enregistrements est 255 
    
    private byte record_length;
    private byte record_number;
    private byte read_sec;
    private byte write_sec;
    private int index;
    private static final byte CLA               = (byte)0x80;
    private byte Lc;
    private byte p1;
    private static final byte INS_SELECT_FILE   = (byte)0xA4;
    private static final byte INS_SUBMIT_CODE   = 0x20;
    private static final byte INS_CLEAR_CARD   = 0x30;
    private static final byte INS_WRITE_RECORD  = (byte)0xD2;
    private static final byte INS_READ_RECORD   = (byte)0xB2;
    private static final byte INS_UPDATE_PIN    = 0x24;
    
    private static final byte SIZE_CODE         = 0x04;
    private static final byte[] initialPIN      = { 0x01, 0x02, 0x03, 0x04};
    private static final byte[] initialIC       = { 0x0A, 0x0B, 0x0C, 0x0D};
    
    private final OwnerPIN PIN;
    private final OwnerPIN IC;
    private final byte Tries_num = 0x07;
    
    private boolean PersFile_selected   = false;
    private boolean ManagFile_selected  = false;
    private boolean SecFile_selected    = false;
    private boolean UserFile_selected   = false;
    private boolean pin_submitted = false;
    private boolean IC_submitted = false;
    /**
     * Installs this applet.
     * 
     * @param bArray
     *            the array containing installation parameters
     * @param bOffset
     *            the starting offset in bArray
     * @param bLength
     *            the length in bytes of the parameter data in bArray
     */
    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new JC_ACOS3();
    }

    /**
     * Only this class's install method should create the applet object.
     */
    protected JC_ACOS3() {
        register();
        PIN = new OwnerPIN( Tries_num, SIZE_CODE );
        PIN.update( initialPIN , (short) 0 , (byte) SIZE_CODE );
        IC  = new OwnerPIN ( Tries_num , SIZE_CODE );
        IC.update( initialIC , (short) 0 , (byte) SIZE_CODE );
        
        this.PersFile_selected = false;
        this.ManagFile_selected = false;
        this.SecFile_selected = false;
        this.UserFile_selected = false;
        this.pin_submitted = false;
        this.IC_submitted = false;
    }

    /**
     * Processes an incoming APDU.
     * 
     * @see APDU
     * @param apdu
     *            the incoming APDU
     */
    public void process(APDU apdu) {
        //Insert your code here
        if(selectingApplet()) return;
        byte[] buf = apdu.getBuffer();
        if(buf[0]!=this.CLA)
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        switch ( buf [ 1 ]) {
        case INS_SELECT_FILE    : { do_select ( apdu ); return;}
        case INS_SUBMIT_CODE    : { do_submit ( apdu );return;}
        case INS_CLEAR_CARD     : { do_clear ( apdu ) ;return;}
        case INS_WRITE_RECORD   : { do_write ( apdu );return;}   
        case INS_READ_RECORD    : { do_read ( apdu );return;}
        case INS_UPDATE_PIN     : { do_update ( apdu );return;}

        default : {
            ISOException.throwIt ( ISO7816 . SW_INS_NOT_SUPPORTED );
            return;
            }
        } // fin switch
    } // fin process ()
    
   
    private void do_update(APDU apdu){
        byte[] buf = apdu.getBuffer();
        if (pin_submitted){
            Lc = buf[4];
            if (Lc == 0x04){
                for(int i=0; i<4; i++){
                    SecFile[(byte)i +4] = buf[(byte)(5+i)];
                }
                PIN.update(buf, (short)5, (byte)4);
                ISOException.throwIt(ISO7816.SW_NO_ERROR);  
            }
            else
                ISOException.throwIt((short)0x6700); //P3 non acceptée
        }
        else
            ISOException.throwIt((short)0x6982);
        pin_submitted =false;
    }
    
    private void do_select(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        if(buf[5]==(byte)0xFF && buf[6]==(byte)0x02){
                    PersFile_selected   = true;
                    ManagFile_selected  = false;
                    SecFile_selected    = false;
                    UserFile_selected   = false;
                    ISOException.throwIt((short) 0x9000);
                }
        else if(buf[5]==(byte)0xFF && buf[6]==(byte)0x03){
                    PersFile_selected   = false;
                    ManagFile_selected  = false;
                    SecFile_selected    = true;
                    UserFile_selected   = false;
                    ISOException.throwIt(ISO7816.SW_NO_ERROR); 
                }
        else if(buf[5]==(byte)0xFF && buf[6]==(byte)0x04){
                    PersFile_selected   = false;
                    ManagFile_selected  = true;
                    SecFile_selected    = false;
                    UserFile_selected   = false;
                    ISOException.throwIt(ISO7816.SW_NO_ERROR); 
                }
        else {
                    index = -1;
                    for(int i=0; i<N_OF_FILES; i++){
                        if(buf[5] == ManagFile[(byte)(i*6 + 4)] && buf[6] == ManagFile[(byte)(i*6 + 5)]){
                            read_sec = ManagFile[(byte)(i*6 + 2)];
                            write_sec = ManagFile[(byte)(i*6 + 3)];
                            record_length = ManagFile[(byte)(i*6)];
                            record_number = ManagFile[(byte)(i*6 +1)];
                            PersFile_selected   = false;
                            ManagFile_selected  = false;
                            SecFile_selected    = false;
                            UserFile_selected   = true;
                            ISOException.throwIt((short) ((short)(0x9100) + (short)i));
                            i = N_OF_FILES; //Stop the for loop
                        }
                        else{
                            int rec_len=(int)ManagFile[(byte)(i*6)];
                            int no_rec=(int)ManagFile[(byte)(i*6 + 1)];
                            index = index +( rec_len * no_rec );
                        }
                }
                if(!UserFile_selected){
                    PersFile_selected   = false;
                    ManagFile_selected  = false;
                    SecFile_selected    = false;
                    UserFile_selected   = false;
                    ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND); 
                }
     }
    }
    private void do_submit(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        if ( buf[2] == 0x06) {
                Lc = buf[4];
                if(Lc == 0x04){
                    if (!PIN.check ( buf , (short) 5 , (byte)4) ){
            		buf [0] = PIN.getTriesRemaining () ;
            		apdu.setOutgoingAndSend (( short)0, (short) 1) ;
            		ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    }
                    else {
                        pin_submitted = true;
                    }
                }
                else {
                    ISOException.throwIt((short)0x6A85); //longueur Lc non acceptée
                }
        }
        else if (buf[2] == 0x07) { 
            Lc = buf[4];
            if(Lc == 0x04){
               	if (!IC.check ( buf , (short) 5 , (byte)4) ){
                    buf [0] = IC.getTriesRemaining () ;
                    apdu.setOutgoingAndSend (( short)0, (short) 1) ;
                    ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
               	}
                else {
                        IC_submitted = true;
                    }
            }
            else {
                ISOException.throwIt((short)0x6A85); //longueur Lc non acceptée
            }
        }
        else {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        
        }
    }
    private void do_clear(APDU apdu) {
        if(!IC_submitted ){
            ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );}
        else {
            PersFile = 0x00;
            SecFile  = new byte[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x01, 0x02, 0x03, 0x04 };
            for(int i=0; i<384 ; i++){
                ManagFile[(byte)i] = (byte)0x00;
            }
            for(int i=0; i<UserFile.length ; i++){
                UserFile[(byte)i] = 0;
            }                  
            N_OF_FILES = 0;
            IC.update(initialIC, (short) 0, (byte) SIZE_CODE);
            PIN.update(initialPIN, (short) 0, (byte) SIZE_CODE);
            ISOException.throwIt (ISO7816.SW_NO_ERROR);
        }
        IC_submitted = false;
    }
    
    private void write_userfile(APDU apdu){
        byte[] buf = apdu.getBuffer();
        Lc = buf[4];
        if (Lc <= record_length){
            p1 = buf[2];
            if (p1 < record_number){
                for(int i=0; i<Lc; i++){
                    UserFile[(byte)(index + (int)p1*Lc + i)] = buf[(byte)i +5];
                }
                ISOException.throwIt(ISO7816.SW_NO_ERROR);
            }
            else{
                ISOException.throwIt((short)0x6A83); //L'enregistrement n'existe pas
            }
        }
        else{
            ISOException.throwIt((short)0x6700); //Lc: erronÃ©
        }
        UserFile_selected = false;
    }
        
        
    private void do_write(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        if(IC_submitted){
            if(PersFile_selected){
                Lc = buf[4];
                if(Lc == 0x01){
                    if(buf[5] <= 0x40){
                        PersFile = buf[5];
                        N_OF_FILES = (int)PersFile;
                        ISOException.throwIt(ISO7816.SW_NO_ERROR);
                    }
                    else{
                        ISOException.throwIt((short)0x6A84); //le maximum autorisé est 64 fichiers
                    }
                }
                else{
                    ISOException.throwIt((short)0x6700); //longueur Lc non acceptée
                }
               PersFile_selected = false; 
            }
            else if(SecFile_selected){
                Lc = buf[4];
                if(Lc == 0x04){
                    p1 = buf[2];
                    if (p1==0x01){
                        for(int i=0; i<4; i++){
                            SecFile[(byte)i +4] = buf[(byte)(5+i)];
                        }
                        PIN.update(buf, (short)5, (byte)4);
                        ISOException.throwIt(ISO7816.SW_NO_ERROR);
                    }
                    else
                        ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
                    
                
                }
                else{
                    ISOException.throwIt((short)0x6700); //longueur Lc non acceptée;
                }
                SecFile_selected = false;
            }
            else if(ManagFile_selected ){
                p1 = buf[2];
                if(PersFile > p1){
                    Lc = buf[4];
                    if(Lc == 0x06){
                        for(int i=0; i<6; i++){
                            ManagFile[(byte)(p1*6 + i)] = buf[(byte)(5+i)];
                        }
                    }
                    else{
                        ISOException.throwIt((short)0x6700); //longueur Lc non acceptée;
                    }
                }
                else{
                    ISOException.throwIt(ISO7816.SW_WRONG_P1P2); //depassement d'enregistrement
                }
                ManagFile_selected = false;
            }
            else
                ISOException.throwIt((short) 0x6985); //Aucun fichier selectionné
        }
        
        else if (UserFile_selected){
                switch (write_sec) {
                case 0x00    : { write_userfile(apdu); return; }
                case (byte)0x80    : { 
                    if(IC_submitted)
                        write_userfile(apdu);
                    else
                        ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    IC_submitted = false;
                    return;}
                case 0x40     : { 
                    if(pin_submitted)
                        write_userfile(apdu);
                    else
                        ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    pin_submitted = false;
                    return;}
                case (byte)0xC0 : { 
                    if(pin_submitted && IC_submitted)
                        write_userfile(apdu);
                    else
                        ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    IC_submitted = false;
                    pin_submitted = false;
                    return;}
                default : {
                    ISOException.throwIt ( ISO7816.SW_FILE_NOT_FOUND );
                    return;
                }
            } // fin switch 
         
        }
        else{
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED); //securité pas verifiée
        }
        IC_submitted =false;
    }  
                

    private void read_userfile(APDU apdu){
        byte[] buf = apdu.getBuffer();
        Lc = buf[4];
        if (Lc <= record_length){
            p1 = buf[2];
            if (p1 < record_number){
                for(int i=0; i<Lc; i++){
                    buf[(byte)i] = UserFile[(byte)(index + (int)p1*record_length + i)];
                }
                apdu.setOutgoingAndSend((short)0, (short)Lc);
            }
            else{
                ISOException.throwIt((short)0x6A83); //L'enregistrement n'existe pas
            }
        }
        else{
            ISOException.throwIt((short)0x6700); //Le: erronÃ©
        }
        UserFile_selected = false;
    }
            
    private void do_read(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        if(PersFile_selected ){
                        Lc = buf[4]; //Le  no data
                        if(Lc <= 0x01){
                            buf[0] = PersFile;
                            apdu.setOutgoingAndSend((short)0, (short)Lc);
                        }
                        else{
                            ISOException.throwIt((short)0x6700); //Le: error
                        }
                        PersFile_selected = false;
                    }
                    
        else if(SecFile_selected){
            if(!IC_submitted){
                ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );}
            else {
                Lc = buf[4]; //Le no data
                if(Lc <= 0x04){
                    p1=buf[2]; //Rec No. P1
                    if (p1==0x00){
                        for(int i=0; i<Lc; i++){
                            buf[(byte)i] = SecFile[(byte)i];
                        }
                        apdu.setOutgoingAndSend((short)0, (short)Lc);
                    }
                    else if (p1==0x01) {
                        for(int i=0; i<Lc; i++){
                            buf[(byte)i] = SecFile[(byte)i + 4];
                        }
                        apdu.setOutgoingAndSend((short)0, (short)Lc);
                    }
                    else{
                        ISOException.throwIt((short)0x6A83); //L'enregistrement n'existe pas
                    }
                }
                else{
                    ISOException.throwIt((short)0x6700); //Le: error
                }
            }
            IC_submitted = false;
            SecFile_selected = false;
        }
        else if(ManagFile_selected){ 
            Lc = buf[4]; //Le
            if(Lc <= 0x06){
                p1 = buf[2]; 
                if(p1 >= 0x00 && p1 < N_OF_FILES){ //indice d'enregistrement
                    for(int i=0; i<Lc; i++){
                        buf[(byte)i] = ManagFile[(byte)((p1)*6 + i)];
                    }
                    apdu.setOutgoingAndSend((short)0, (short)Lc);
                }
                else{
                    ISOException.throwIt((short)0x6A83); //L'enregistrement n'existe pas
                }
            }
            else{
                ISOException.throwIt((short)0x6700); //Le: erronÃ©
            }
            ManagFile_selected = false;
        }        
        else if(UserFile_selected){
            switch (read_sec) {
                case 0x00    : { read_userfile(apdu); return; }
                case (byte)0x80    : { 
                    if(IC_submitted)
                        read_userfile(apdu);
                    else
                        ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    IC_submitted = false;
                    return;}
                case 0x40     : { 
                    if(pin_submitted)
                        read_userfile(apdu);
                    else
                        ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    pin_submitted = false;
                    return;}
                case (byte)0xC0 : { 
                    if(pin_submitted && IC_submitted)
                        read_userfile(apdu);
                    else
                        ISOException.throwIt ( ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED );
                    IC_submitted = false;
                    pin_submitted = false;
                    return;}
                default : {
                    ISOException.throwIt ( ISO7816.SW_FILE_NOT_FOUND );
                    return;
                }
            } // fin switch   
        }
    }
}