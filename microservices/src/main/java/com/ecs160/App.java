package com.ecs160;

public class App 
{
    public static void main( String[] args )
    {
        try {
            // initialize framework 
            MicroserviceFramework framework = new MicroserviceFramework();
            // launch services
            framework.launch(8080);

        } catch (Exception e) {
            System.err.println("Failed to start services: " + e.getMessage());
            e.printStackTrace();
        }
    }
}