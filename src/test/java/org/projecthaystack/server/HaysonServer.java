package org.projecthaystack.server;

// import org.apache.catalina.startup.Tomcat;
// import org.apache.catalina.Context;
// import org.apache.catalina.LifecycleException;

import org.projecthaystack.server.*;

public class HaysonServer 
{
    // private Tomcat tomcat;
    private int port;
    private boolean isRunning = false;
    
    public HaysonServer() 
    {
        this(8080);
    }
    
    public HaysonServer(int port) 
    {
        this.port = port;
    }
    
    /**
     * Start the server programmatically
     */
    public void start() //throws LifecycleException 
    {
        if (isRunning) return;
        
        // tomcat = new Tomcat();
        // tomcat.setPort(port);
        
        // Context context = tomcat.addContext("/", new java.io.File(".").getAbsolutePath());
        // tomcat.addServlet(context, "haysonRead", new HServlet());
        // context.addServletMappingDecoded("/hayson/read", "haysonRead");
        
        // tomcat.start();
        // isRunning = true;
        
        System.out.println("Starting Hayson server on port " + port);
        System.out.println("Endpoint: GET /hayson/read");
        System.out.println("Supports both JSON v3 and v4 (Hayson) based on Accept header");
        System.out.println("Example usage:");
        System.out.println("  curl -H \"Accept: application/json; version=3\" http://localhost:" + port + "/hayson/read");
        System.out.println("  curl -H \"Accept: application/json; version=4\" http://localhost:" + port + "/hayson/read");
    }
    
    /**
     * Stop the server programmatically
     */
    public void stop() //throws LifecycleException 
    {
        // if (tomcat != null && isRunning) 
        // {
        //     tomcat.stop();
        //     tomcat.destroy();
        //     isRunning = false;
        //     System.out.println("Stopped Hayson server on port " + port);
        // }
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() 
    {
        return isRunning;
    }
    
    /**
     * Get the port the server is running on
     */
    public int getPort() 
    {
        return port;
    }
    
    /**
     * Main method for command line execution
     */
    public static void main(String[] args) 
    {
        int port = 8080;
        
        // Parse command line arguments for port
        if (args.length > 0) 
        {
            try 
            {
                port = Integer.parseInt(args[0]);
            } 
            catch (NumberFormatException e) 
            {
                System.err.println("Invalid port number, using default port 8080");
            }
        }
        
        HaysonServer server = new HaysonServer(port);
        try 
        {
            server.start();
            
            // Keep the main thread alive
            Thread.currentThread().join();
            
        } 
        catch (Exception e) 
        {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Example usage for automated test harness
     */
    // public static void mainExample() 
    // {
    //     // Example of how to use in automated tests
    //     HaysonServer server = new HaysonServer(0); // 0 means random port
        
    //     try 
    //     {
    //         server.start();
            
    //         System.out.println("Server running on port " + server.getPort());
            
    //         // Your test code here - make HTTP requests to server.getPort()
    //         // Example: 
    //         // HttpClient client = HttpClient.newHttpClient();
    //         // HttpRequest request = HttpRequest.newBuilder()
    //         //     .uri(URI.create("http://localhost:" + server.getPort() + "/hayson/read"))
    //         //     .header("Accept", "application/json; version=4")
    //         //     .GET()
    //         //     .build();
            
    //         Thread.sleep(5000); // Let it run for 5 seconds
            
    //         // Stop the server when done
    //         server.stop();
            
    //     } 
    //     catch (Exception e) 
    //     {
    //         e.printStackTrace();
    //     }
    // }
}
