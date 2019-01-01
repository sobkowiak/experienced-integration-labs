# Homework - how to test?

## Prerequisities 

- JDK 8 
- Maven 3.x
- Download `jboss-fuse-karaf-6.3.0.redhat-187.zip` from https://access.redhat.com
- Ensure you have `curl`or `httpie`(https://httpie.org/) installed

## Build & deploy

1. Build the project
    
    - Open a Terminal and locate the `parent` folder and build the project using
        
          mvn clean install
    
    - Next locate the `core` folder and build it using the above command. This will build all of the projects below it including:

      - Artifacts - which has all of the files necessary for starting the web services.
      - Services  - which has the NextGate test server and an OSGi service connection pool to the AMQ broker
      - Inbound   - which runs a REST server and sends the message to a broker
      - Xlate     - which translates the message from the Person format to the NextGate format
      - Outbound  - which reads the final message from the broker and sends it to the NextGate sever
      - Features  - which builds a features file for easy installation of the entire project on Fuse
    

1. Deploy on Fuse

    - Open a Terminal and locate the jboss-fuse-karaf-6.3.0.redhat-187.zip
    - Extract the file
    
          unzip jboss-fuse-karaf-6.3.0.redhat-187.zip -d .
          cd jboss-fuse-6.3.0.redhat-187
          
    - Activate the `admin` user
    
          echo "admin=admin,admin,manager,viewer,Monitor, Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser" >> etc/users.properties
          
    - Create config file for `NextGate` test service
    
          echo "nextgate.ws.uri=http://localhost:9099/PersonEJBService/PersonEJB" >>  etc/com.davita.esb.empi.addorupdateempi.cfg
          
      This file is used by the Config Admin to configure the NextGate test service. The service will be available under this url
      
    - Create config file for `outbound` integration route
    
          echo "nextgate.url=http://localhost:9099/PersonEJBService/PersonEJB" >>  etc/com.customer.outbound.cfg
    
      This file will be used by Config Admin to configure the route calling the Nextgate web service 
      
    - Go to the `bin` folder and run `fuse`
    
          cd bin
          ./fuse
    - Add the feature url and install the `customer` fearure. Ensure all bundles are up and running
    
          JBossFuse:karaf@root> features:addurl mvn:com.customer.app/customer-features/1.0-SNAPSHOT/xml/features
      
      This command tells Fuse where in the Maven repo to find the feature file with te customer app. 
      
    - Next install the custommer app using following command	  
	        
          JBossFuse:karaf@root> features:install customer-features
          
      This will install all of the core bundles and their dependencies. To see all of the bundles that were installed and if they started properly you can run the command:  
      
          JBossFuse:karaf@root> osgi:list 
          START LEVEL 100 , List Threshold: 50
          ID     State         Blueprint      Spring    Level  Name
          [  14] [Active     ] [Created     ] [       ] [   50] JBoss Fuse :: ESB :: Commands (6.3.0.redhat-187)
          .........
          [ 323] [Active     ] [            ] [       ] [   85] Customer :: Application :: Core :: Artifacts (1.0.0.SNAPSHOT)
          [ 324] [Active     ] [Created     ] [       ] [   85] Customer :: Application :: Core :: Services :: Integration-Test-Server (1.0.0.SNAPSHOT)
          [ 325] [Active     ] [            ] [Started] [   85] Customer :: Application :: Core :: Services :: MQ-Service (1.0.0.SNAPSHOT)
          [ 326] [Active     ] [            ] [Started] [   85] Customer :: Application :: Core :: Inbound (1.0.0.SNAPSHOT)
          [ 327] [Active     ] [            ] [Started] [   85] Customer :: Application :: Core :: XLate (1.0.0.SNAPSHOT)
          [ 328] [Active     ] [            ] [Started] [   85] Customer :: Application :: Core :: Outbound (1.0.0.SNAPSHOT)

    - Set the logging level of `com.redhat.usecase` to DEBUG.
	
          JBossFuse:karaf@root> log:set DEBUG com.redhat.usecase
          
      This activates a logger `com.redhat.usecase` used to log from the Camel route, e.g.  
      
          <log message="Unmarshalling xml to object..." loggingLevel="DEBUG" logName="com.redhat.usecase" />

After successfull deployment of the `custommer` the services will be listening at

- The REST service at http://localhost:9098/cxf/demos
- The SOAP Nextgate service at http://localhost:9099/PersonEJBService/PersonEJB
  
  Please call the url http://localhost:9099/PersonEJBService/PersonEJB?wsdl to see the wsdl and check whether the service has been successfully deployed
  
## Testing 

- Locate the folder with the test data of the `inbound` project and cd into the folder

      cd inbound/src/test/data
      
- Call the REST service using one of follwimg commands

      curl -X POST -H "Accept: application/xml" -H "Content-Type: application/xml"   http://localhost:9098/cxf/demos/match  -d @Person.xml
      
      http --all POST   http://localhost:9098/cxf/demos/match  Accept:application/xml Content-Type:application/xml  @Person.xml
      
      
  You should get following result
  
      <?xml version="1.0" encoding="UTF-8" standalone="yes"?><ESBResponse xmlns="http://www.response.app.customer.com"><BusinessKey>c4e82e2e-d095-4cbd-9aa8-728949fa0920</BusinessKey><Published>true</Published><Comment>MATCH</Comment></ESBResponse>

- Check the log file in fuse

      JBossFuse:karaf@root> log:display
      
 
