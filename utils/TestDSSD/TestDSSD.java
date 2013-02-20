import java.net.*;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Message;
import org.xbill.DNS.Section;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Header;
import org.xbill.DNS.Flags;


public class TestDSSD {
	public static void main(String args[]) throws UnknownHostException {
		resolveService(args[0]);
	}

	TestDSSD() {
		return;
	}

	public static void resolveService(String name) throws UnknownHostException {
		InetAddress addr = InetAddress.getLocalHost();
		int port = 53;

		try {
			byte[] sendbuf = generateRequest(name);
			if (sendbuf == null) {
				System.err.println("error: sendbuf is null");
				return;
			}

			DatagramPacket sendpack = new DatagramPacket(sendbuf, sendbuf.length, addr, port);

			byte[] recbuf = new byte[1024];
			DatagramPacket recpack = new DatagramPacket(recbuf, recbuf.length);
			DatagramSocket sock = new DatagramSocket();
			sock.setSoTimeout(10000);

			long start_time = System.currentTimeMillis();
			sock.send(sendpack);

			// Timeout Breaks Here!
			sock.receive(recpack);
			long elapsed_millis = System.currentTimeMillis() - start_time;

			Message m = new Message(recpack.getData());
			InetAddress result = parseResponse(m);

			System.out.println("received " + result + " for " + name + " in " + elapsed_millis + " milliseconds.");

		} catch (SocketTimeoutException e) {
			// timed out! :-(
			System.err.println("error: socket timed out");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static byte[] generateRequest(String name) {
		try {
			if (!name.endsWith(".")) {
				name = name.concat(".");
			}

			if (!name.endsWith("dssd.")) {
				name = name.concat("dssd.");
			}

			Name n = new Name(name);
			Record query = Record.newRecord(n, Type.A, DClass.IN);
			Message request = Message.newQuery(query);
			return request.toWire();
		} catch (Exception e) {
			System.err.println("IGS error: could not generate request");
			return null;
		}
	}

	public static InetAddress parseResponse(Message response) {
		Header header = response.getHeader();

		if (!header.getFlag(Flags.QR)) {
			System.err.println("error: response not a QR");
			return null;
		}
		if (header.getRcode() != Rcode.NOERROR) {
			System.err.println("error: header Rcode is " + header.getRcode());
			return null;
		}
		Record question = response.getQuestion();

		// sanity check on question here...

		Record[] records = response.getSectionArray(Section.ANSWER);

		// simplest alg: just return the first valid A record...
		Record answer = null;
		for(int i = 0; i < records.length; i++) {
			answer = records[i];
			if (answer.getType() != Type.A) {
				continue;
			}

			byte[] address = answer.rdataToWireCanonical();
			try {
				InetAddress result = InetAddress.getByAddress(address);
				return result;
			} catch (UnknownHostException e) {
				continue;
			}
		}

		return null;
	}
}
