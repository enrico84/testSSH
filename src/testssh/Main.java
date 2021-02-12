package testssh;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class Main {
	public static boolean testConnection(String host, String user, String pwd) {
		System.out.println();
		System.out.println("---------------------");
		System.out.println("Attempting to connect to "+host);
		
		Session session = null;
		Channel channel = null;
		try
		{
			JSch jsch=new JSch();
			session=jsch.getSession(user, host, 22);
			session.setPassword(pwd);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(20000);
			
			channel=session.openChannel("exec");
		      ((ChannelExec)channel).setCommand("echo hello "+session.getHost());
	
		      channel.setInputStream(null);
		      ((ChannelExec)channel).setOutputStream(System.out);
		      //((ChannelExec)channel).setErrStream(System.err);
	
		      InputStream in=channel.getInputStream();
	
		      channel.connect();
	
		      int exitStatus = -1;
		      byte[] tmp=new byte[1024];
		      while(true){
		        while(in.available()>0){
		          int i=in.read(tmp, 0, 1024);
		          if(i<0)break;
		          System.out.print(new String(tmp, 0, i));
		        }
		        if(channel.isClosed()){
		          if(in.available()>0) continue;
		          exitStatus = channel.getExitStatus();
		          System.out.println("exit-status: "+exitStatus);
		          break;
		        }
		        try{Thread.sleep(1000);}catch(Exception ee){}
		      }
		      return exitStatus==0;
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			return false;
		}
		finally
		{
			if (channel!=null) try { channel.disconnect(); } catch (Throwable t) {}
			if (session!=null) try { session.disconnect(); } catch (Throwable t) {}
		}
	}
	
	private static Properties readProperties() {
		System.out.println("Reading properties...");
		
		Properties p = new Properties();
		InputStream is = null;
		try
		{
			is = new FileInputStream(new File("./config.properties"));
			p.load(is);
			return p;
		}
		catch (Throwable t)
		{
			throw new RuntimeException("Unable to read properties", t);
		}
		finally
		{
			if (is!=null) try { is.close(); } catch (Throwable t) {}
		}
	}
	
	private static String getUsername(Properties p) {
		return p.getProperty("username");
	}
	
	private static String getPassword(Properties p) {
		return p.getProperty("password");
	}
	
	private static List<HostInfo> getHosts(Properties p) {
		List<HostInfo> hostInfos = new ArrayList<HostInfo>();
		
		int i = 0;
		while (true) {
			String name = p.getProperty("host."+i+".name");
			if (name==null)
				break;
			
			String desc = p.getProperty("host."+i+".desc");
			
			HostInfo info = new HostInfo();
			info.host = name;
			info.description = desc;
			
			hostInfos.add(info);
			
			i++;
		}
		
		return hostInfos;
	}

	private static void check(String user, String pass, List<HostInfo> hosts) {
		System.out.println("Checking properties...");
		
		if (user==null)
			throw new RuntimeException("User null");
		if (pass==null)
			throw new RuntimeException("Pass null");
		if (hosts==null || hosts.isEmpty())
			throw new RuntimeException("Hosts empty");
	}
	
	public static void main(String[] args) {
		Properties p = readProperties();
		
		String user = getUsername(p);
		String pass = getPassword(p);
		List<HostInfo> hosts = getHosts(p);
		check(user, pass, hosts);
		
		Map<HostInfo, Boolean> tests = new LinkedHashMap<HostInfo, Boolean>();
		for (HostInfo info: hosts) {
			boolean b = testConnection(info.host, user, pass);
			tests.put(info, b);
		}
	
		System.out.println();
		System.out.println("======================");
		for (Map.Entry<HostInfo, Boolean> test: tests.entrySet()) {
			String result = test.getValue() ? "ok" : "KO";
			HostInfo info = test.getKey();
			System.out.println(result+" - "+info.host+" ("+info.description+")");
		}
	}

	private static class HostInfo {
		public String host;
		public String description;
	}
}
