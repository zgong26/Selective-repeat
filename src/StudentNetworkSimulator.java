import java.util.*;
import java.io.*;

public class StudentNetworkSimulator extends NetworkSimulator
{
    /*
     * Predefined Constants (static member variables):
     *
     *   int MAXDATASIZE : the maximum size of the Message data and
     *                     Packet payload
     *
     *   int A           : a predefined integer that represents entity A
     *   int B           : a predefined integer that represents entity B 
     *
     * Predefined Member Methods:
     *
     *  void stopTimer(int entity): 
     *       Stops the timer running at "entity" [A or B]
     *  void startTimer(int entity, double increment): 
     *       Starts a timer running at "entity" [A or B], which will expire in
     *       "increment" time units, causing the interrupt handler to be
     *       called.  You should only call this with A.
     *  void toLayer3(int callingEntity, Packet p)
     *       Puts the packet "p" into the network from "callingEntity" [A or B]
     *  void toLayer5(String dataSent)
     *       Passes "dataSent" up to layer 5
     *  double getTime()
     *       Returns the current time in the simulator.  Might be useful for
     *       debugging.
     *  int getTraceLevel()
     *       Returns TraceLevel
     *  void printEventList()
     *       Prints the current event list to stdout.  Might be useful for
     *       debugging, but probably not.
     *
     *
     *  Predefined Classes:
     *
     *  Message: Used to encapsulate a message coming from layer 5
     *    Constructor:
     *      Message(String inputData): 
     *          creates a new Message containing "inputData"
     *    Methods:
     *      boolean setData(String inputData):
     *          sets an existing Message's data to "inputData"
     *          returns true on success, false otherwise
     *      String getData():
     *          returns the data contained in the message
     *  Packet: Used to encapsulate a packet
     *    Constructors:
     *      Packet (Packet p):
     *          creates a new Packet that is a copy of "p"
     *      Packet (int seq, int ack, int check, String newPayload)
     *          creates a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and a
     *          payload of "newPayload"
     *      Packet (int seq, int ack, int check)
     *          chreate a new Packet with a sequence field of "seq", an
     *          ack field of "ack", a checksum field of "check", and
     *          an empty payload
     *    Methods:
     *      boolean setSeqnum(int n)
     *          sets the Packet's sequence field to "n"
     *          returns true on success, false otherwise
     *      boolean setAcknum(int n)
     *          sets the Packet's ack field to "n"
     *          returns true on success, false otherwise
     *      boolean setChecksum(int n)
     *          sets the Packet's checksum to "n"
     *          returns true on success, false otherwise
     *      boolean setPayload(String newPayload)
     *          sets the Packet's payload to "newPayload"
     *          returns true on success, false otherwise
     *      int getSeqnum()
     *          returns the contents of the Packet's sequence field
     *      int getAcknum()
     *          returns the contents of the Packet's ack field
     *      int getChecksum()
     *          returns the checksum of the Packet
     *      int getPayload()
     *          returns the Packet's payload
     *
     */

    /*   Please use the following variables in your routines.
     *   int WindowSize  : the window size
     *   double RxmtInterval   : the retransmission timeout
     *   int LimitSeqNo  : when sequence number reaches this value, it wraps around
     */

    public static final int FirstSeqNo = 0;
    private int WindowSize;
    private double RxmtInterval;
    private int LimitSeqNo;
    
    // Add any necessary class variables here.  Remember, you cannot use
    // these variables to send messages error free!  They can only hold
    // state information for A or B.
    // Also add any necessary methods (e.g. checksum of a String)
    private int seqNoA;
    private int ackNoA;
    private int checkSum;
    private int lastSeq;//last seq received by layer5 on receiver side
    //array to track each pack sent time for selective
    private Queue<Packet> senderBuffer;//sender senderBuffer to store out-of-window packets
    private Queue<Packet> senderWindow;//used to keep track of packets in the sender window
    private PriorityQueue<Packet> receiverBuffer;//buffer on receiver side
    
    // This is the constructor.  Don't touch!
    public StudentNetworkSimulator(int numMessages,
                                   double loss,
                                   double corrupt,
                                   double avgDelay,
                                   int trace,
                                   int seed,
                                   int winsize,
                                   double delay)
    {
        super(numMessages, loss, corrupt, avgDelay, trace, seed);
	WindowSize = winsize;
	LimitSeqNo = winsize*2; // set appropriately; assumes SR here!
	RxmtInterval = delay;
    }

    // This routine will be called whenever the upper layer at the sender [A]
    // has a message to send.  It is the job of your protocol to insure that
    // the data in such a message is delivered in-order, and correctly, to
    // the receiving upper layer.
    protected void aOutput(Message message)
    {
    	//create a packet including seq, message, etc and put it into the queue
    	//calculate checksum by adding sequence number, ack number and each char of payload together
    	checkSum = seqNoA + ackNoA;
    	String payload = message.getData();
    	for(char c: payload.toCharArray())
    		checkSum += (int) c;
    	Packet newPack = new Packet(seqNoA, ackNoA, checkSum, payload);
    	//while the window is not full
    	if(senderWindow.size() < WindowSize) {
    		senderWindow.add(newPack);
    		//sendWindowPackets();
    		startTimer(0, RxmtInterval);
    		toLayer3(0, newPack);
    	}
    	else {
    		senderBuffer.add(newPack); //otherwise add to the senderBuffer outside the window
    	}
    	
    	seqNoA++;
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by a B-side procedure)
    // arrives at the A-side.  "packet" is the (possibly corrupted) packet
    // sent from the B-side.
    protected void aInput(Packet packet)
    {
    	stopTimer(0);
    	//check if checksum is correct
    	int seq = packet.getSeqnum();
    	int ack = packet.getAcknum();
    	int calculatedCheck = seq + ack;
    	String payload = packet.getPayload();
    	for(char c: payload.toCharArray())
    		calculatedCheck += (int) c;
    	if(senderWindow.isEmpty())
    		return;
    	//if corrupted, do nothing
    	if(calculatedCheck != packet.getChecksum()){
    		
    	}
    	//if duplicated(it actually means the ack(seq) is one smaller than the current oldest packet), then retransmit
    	else if(seq < senderWindow.peek().getSeqnum()) {
    		toLayer3(0, senderWindow.peek());//retransmit
    		startTimer(0, RxmtInterval);
    	}
    	else {
    		//When receiving a new ack, remove those acked packs from the window and push packs to the window from senderBuffer if there are any
    		while(!senderWindow.isEmpty() && senderWindow.peek().getSeqnum() <= seq)
    			senderWindow.poll();
    		//push packets from senderBuffer to window if available
    		while(senderWindow.size() < WindowSize && !senderBuffer.isEmpty()) {
    			Packet newpck = senderBuffer.poll();
    			senderWindow.add(newpck);
    			toLayer3(0, newpck);
    			startTimer(0, RxmtInterval);
    		}
    	}
    }
    
    // This routine will be called when A's timer expires (thus generating a 
    // timer interrupt). You'll probably want to use this routine to control 
    // the retransmission of packets. See startTimer() and stopTimer(), above,
    // for how the timer is started and stopped. 
    protected void aTimerInterrupt()
    {
    	stopTimer(0);
    	toLayer3(0, senderWindow.peek());//resend the oldest one in the window
    	startTimer(0, RxmtInterval);
    }
    
    // This routine will be called once, before any of your other A-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity A).
    protected void aInit()
    {
    	seqNoA = FirstSeqNo;
    	ackNoA = 0;
    	senderBuffer = new LinkedList<Packet>();
    	senderWindow = new LinkedList<Packet>();
    }
    
    // This routine will be called whenever a packet sent from the B-side 
    // (i.e. as a result of a toLayer3() being done by an A-side procedure)
    // arrives at the B-side.  "packet" is the (possibly corrupted) packet
    // sent from the A-side.
    protected void bInput(Packet packet)
    {
    	//check if checksum is correct
    	int seq = packet.getSeqnum();
    	int ack = packet.getAcknum();
    	int calculatedCheck = seq + ack;
    	String payload = packet.getPayload();
    	for(char c: payload.toCharArray())
    		calculatedCheck += (int) c;
    	//if corrupted or duplicated, do nothing
    	if(calculatedCheck != packet.getChecksum()) {
    		return;
    	}
    	//if duplicated, drop and re-ack
    	else if(receiverBuffer.contains(packet) || seq <= lastSeq){
    		toLayer3(1, new Packet(lastSeq, 1, lastSeq + 1));
    		return;
    	}
    	//if new, just put into the buffer, not ack until buffer is in order
    	else{
    		receiverBuffer.add(packet);
    	}
    	//check whether seq in the priority queue is 1 different from each other(max seq - min seq = size)
    	//if true, dump everything in pq to layer 5
    	if(receiverBuffer.peek().getSeqnum() - lastSeq == 1) {
    		while(!receiverBuffer.isEmpty() && receiverBuffer.peek().getSeqnum() - lastSeq == 1) {
    			Packet temp = receiverBuffer.poll();
    			toLayer5(temp.getPayload());
    			lastSeq = temp.getSeqnum();
    		}
    		toLayer3(1, new Packet(lastSeq, 1, lastSeq + 1));
    	}
    }
    
    // packet sequence number comparator that will be used in priority queue on receiver side
    private class seqComparator implements Comparator<Packet>{

		@Override
		public int compare(Packet p1, Packet p2) {
			if(p1.getSeqnum() > p2.getSeqnum())
				return 1;
			else if(p1.getSeqnum() < p2.getSeqnum())
				return -1;
			return 0;
		}
	}
    
    // This routine will be called once, before any of your other B-side 
    // routines are called. It can be used to do any required
    // initialization (e.g. of member variables you add to control the state
    // of entity B).
    protected void bInit()
    {
    	receiverBuffer = new PriorityQueue<Packet>(new seqComparator());
    	lastSeq = -1;
    }
    
    
    // Use to print final statistics
    protected void Simulation_done()
    {
    	// TO PRINT THE STATISTICS, FILL IN THE DETAILS BY PUTTING VARIBALE NAMES. DO NOT CHANGE THE FORMAT OF PRINTED OUTPUT
    	System.out.println("\n\n===============STATISTICS=======================");
    	System.out.println("Number of original packets transmitted by A:" + "<YourVariableHere>");
    	System.out.println("Number of retransmissions by A:" + "<YourVariableHere>");
    	System.out.println("Number of data packets delivered to layer 5 at B:" + "<YourVariableHere>");
    	System.out.println("Number of ACK packets sent by B:" + "<YourVariableHere>");
    	System.out.println("Number of corrupted packets:" + "<YourVariableHere>");
    	System.out.println("Ratio of lost packets:" + "<YourVariableHere>" );
    	System.out.println("Ratio of corrupted packets:" + "<YourVariableHere>");
    	System.out.println("Average RTT:" + "<YourVariableHere>");
    	System.out.println("Average communication time:" + "<YourVariableHere>");
    	System.out.println("==================================================");

    	// PRINT YOUR OWN STATISTIC HERE TO CHECK THE CORRECTNESS OF YOUR PROGRAM
    	System.out.println("\nEXTRA:");
    	// EXAMPLE GIVEN BELOW
    	//System.out.println("Example statistic you want to check e.g. number of ACK packets received by A :" + "<YourVariableHere>"); 
    }	

}
