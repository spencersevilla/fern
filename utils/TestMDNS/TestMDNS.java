import java.net.*;

public class TestMDNS {
	public static void main(String args[]) throws UnknownHostException {
		String name = args[0];
		long start_time = System.currentTimeMillis();
		InetAddress addr = InetAddress.getByName(name);
		long elapsed_time = System.currentTimeMillis() - start_time;
		System.out.println("resolution took: " + elapsed_time + " milliseconds.");
	}

	TestMDNS() {
		return;
	}
}
