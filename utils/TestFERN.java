import java.io.*;
import java.net.InetAddress;
import com.spencersevilla.fern.*;
import com.spencersevilla.fern.CommandLineParser;

public class TestFERN {

    public static void main(String[] args) throws Exception {
		FERNInterface fern = new FERNInterface();

		InetAddress a = InetAddress.getByName("127.0.0.1");
		byte[] addr = a.getAddress();

		System.out.println("Test: Class creation");
		Name n = new Name("a.b.c");
		Record r = new Record(n, Type.A, DClass.IN, 0, addr);
		Service s = new Service(n);
		Request req = new Request(n);
		Registration reg = new Registration(n, r);
		System.out.println("Test Passed!");
	}
}