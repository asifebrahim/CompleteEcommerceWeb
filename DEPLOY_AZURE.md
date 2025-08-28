# Deploying EcommerceFresh to Azure (step-by-step)

This guide shows a simple, reproducible way to deploy the Spring Boot application in this repository to Azure using:
- Azure Database for MySQL (Flexible Server)
- Azure App Service (Linux) running the assembled JAR

It is written for clarity — follow each step and run the commands in a bash shell.

Prerequisites
- An Azure subscription and ability to create resources
- Azure CLI installed and logged in (az login)
- Maven or the included wrapper (./mvnw) installed and executable
- Java JDK 17+ installed locally for building
- Basic comfort running terminal commands

Overview
1. Build the app JAR locally
2. Create a resource group
3. Create an Azure Database for MySQL server (Flexible Server)
4. Create the application database and configure firewall rules
5. Create an App Service plan and a Web App (Java runtime)
6. Set environment variables (SPRING_DATASOURCE_*) in the Web App
7. Deploy the JAR using `az webapp deploy`
8. Tail logs and verify the app

Notes about connection strings
- Spring/Hikari requires a JDBC URL that starts with `jdbc:`. Do NOT pass `mysql://...` directly.
- Azure MySQL admin usernames typically must include the server name: `adminuser@servername`.
- For quick testing you can disable strict SSL, but for production configure SSL/TLS properly.

1) Build the JAR

From the project root:

bash -lc "./mvnw clean package -DskipTests"

After successful build you will have a JAR in `target/` (for example `target/EcommerceFresh-0.0.1-SNAPSHOT.jar`).

2) Create a resource group

Replace values where indicated.

bash -lc "AZ_RG=my-ecommerce-rg
AZ_LOC=eastus
az group create --name $AZ_RG --location $AZ_LOC"

3) Create an Azure Database for MySQL (Flexible Server)

Pick a server name and admin credentials. Example shown below uses small SKU for testing.

bash -lc "MYSQL_SERVER_NAME=my-ef-mysql-$(date +%s)
MYSQL_ADMIN_USER=mysqladmin
MYSQL_ADMIN_PASS='StrongP@ssw0rd!'
az mysql flexible-server create \
  --resource-group $AZ_RG \
  --name $MYSQL_SERVER_NAME \
  --admin-user $MYSQL_ADMIN_USER \
  --admin-password $MYSQL_ADMIN_PASS \
  --sku-name Standard_B1ms \
  --location $AZ_LOC \
  --version 8.0 --storage-size 32"

Note: This creates a public endpoint by default. For production you should use VNet integration or restrict firewall rules.

4) Create the application database and allow app access

Create the database used by your app (employee_directory):

bash -lc "az mysql flexible-server db create --resource-group $AZ_RG --server-name $MYSQL_SERVER_NAME --database-name employee_directory"

Allow connections from Azure services (convenient for App Service). This adds a firewall rule that permits Azure's internal IPs to connect:

bash -lc "az mysql flexible-server firewall-rule create --resource-group $AZ_RG --server-name $MYSQL_SERVER_NAME --name AllowAzureIps --start-ip-address 0.0.0.0 --end-ip-address 0.0.0.0"

If you prefer to be strict, obtain the outbound IP addresses of your App Service after creation and create a rule per IP.

Get the fully qualified host name (FQDN) for the server — you will need it for the JDBC URL:

bash -lc "MYSQL_HOST=$(az mysql flexible-server show --resource-group $AZ_RG --name $MYSQL_SERVER_NAME --query fullyQualifiedDomainName -o tsv)
echo $MYSQL_HOST"

Important: The admin username for Azure MySQL must be used like `mysqladmin@$MYSQL_SERVER_NAME` when connecting from a client.

5) Create an App Service plan and Web App (Linux + Java)

Pick an app name that is globally unique.

bash -lc "APP_NAME=my-ecommerce-app-$(date +%s)
PLAN_NAME=ef-plan
az appservice plan create --name $PLAN_NAME --resource-group $AZ_RG --is-linux --sku B1
az webapp create --resource-group $AZ_RG --plan $PLAN_NAME --name $APP_NAME --runtime 'JAVA|17-java17'"

Enable "Always On" (recommended for long-running apps / background tasks):

bash -lc "az webapp config set --resource-group $AZ_RG --name $APP_NAME --always-on true"

6) Configure application settings (environment variables)

Construct the JDBC URL. Example (adjusted for convenience):

- JDBC URL (quick test, disables strict SSL — not for production):

  jdbc:mysql://<MYSQL_HOST>:3306/employee_directory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

Set Spring environment variables in the Web App (replace placeholders):

bash -lc "JDBC_URL=\"jdbc:mysql://$MYSQL_HOST:3306/employee_directory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC\"
ADMIN_USER=\"$MYSQL_ADMIN_USER@$MYSQL_SERVER_NAME\"
ADMIN_PASS=\"$MYSQL_ADMIN_PASS\"
az webapp config appsettings set \
  --resource-group $AZ_RG \
  --name $APP_NAME \
  --settings SPRING_DATASOURCE_URL=$JDBC_URL SPRING_DATASOURCE_USERNAME=$ADMIN_USER SPRING_DATASOURCE_PASSWORD=$ADMIN_PASS SPRING_JPA_HIBERNATE_DDL_AUTO=update"

Notes:
- The username must include the server (e.g. mysqladmin@my-ef-mysql-123).
- For production, prefer a limited-privilege DB user instead of the admin account.

7) Deploy the JAR to the Web App

Use the Azure CLI to deploy the JAR produced in step 1.

bash -lc "az webapp deploy --resource-group $AZ_RG --name $APP_NAME --src-path target/*.jar"

Set a startup command so App Service runs your JAR (explicit startup command). Example:

bash -lc "az webapp config set --resource-group $AZ_RG --name $APP_NAME --startup-file 'java -jar /home/site/wwwroot/*.jar'"

8) View logs and tail output

Tail the app logs from your terminal to see Hibernate / DB connection messages:

bash -lc "az webapp log tail --resource-group $AZ_RG --name $APP_NAME"

9) Common troubleshooting
- Communications link failure / connection refused:
  - Verify $MYSQL_HOST is correct and MySQL server is running.
  - Confirm firewall rules allow the App Service to reach the MySQL server.
  - Ensure the username includes `@<servername>` when connecting to Azure MySQL.
  - For race conditions, set a small restart delay in App Service or add a startup probe in your jar that waits for DB readiness.

- JDBC URL issues: make sure the URL starts with `jdbc:` and includes the DB name and any required params.

- SSL errors: Azure MySQL often requires SSL. For production change `useSSL=false` to the appropriate SSL params and add certificate validation or use `ssl-mode=REQUIRED` in the connection string.

10) Secure secrets and production tips
- Do NOT keep DB passwords in source. Use Azure App Configuration, Key Vault, or set app settings only in the Azure Portal (not in code).
- Create a dedicated DB user (not the admin) with only required privileges.
- Use VNet Integration to keep DB private and accessible only from your App Service.
- Turn on automated backups for the MySQL server.

11) (Optional) Continuous deployment from GitHub
- Use GitHub Actions to build and deploy on push. Azure provides a quick-start GitHub Action or you can use `azure/webapps-deploy` action to push the JAR.

Example (high level):
- Create a GitHub Actions workflow that runs `./mvnw package` and then uses `azure/webapps-deploy@v2` to push `target/*.jar` to the App Service.

Closing notes
- This guide aims for clarity and a working dev/test deployment. For production, tighten firewall rules, enforce SSL, store secrets in Key Vault, and use managed identities where possible.

If you want, I can:
- Add a small `start.sh` to the repo that waits for the DB and starts the jar (helps prevent race conditions), or
- Create a GitHub Actions workflow file that automatically builds and deploys to the App Service.

