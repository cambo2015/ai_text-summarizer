# Alpha Grape

## Getting started

### Using a Docker setup
To get started:
1. clone this repository 
2. create the localhost+1.pem file ``` mkcert localhost 127.0.0.1 ::1 ``` in the /backend root directory
3. create the localhost+1-key.pem file.
3. You may need to rename the .pem files. 
Make sure they are named the same names as the file above or 
you will have to change the application.properties file.
4. create keystore.p12 file by running this command ```bash
openssl pkcs12 -export \
-out keystore.p12 \
-inkey localhost+1-key.pem \
-in localhost+1.pem \
-name tomcat ```
5. Create 3 subscription tiers with metered overage usage using Stripe and put the price_id's in the .env.prod file
5. You must have Docker installed.
6. Add necessary env variables to .env.prod file
7. run ``` docker compose --env-file .env.prod up --build ``` in the terminal.

## ENV VARS NEEDED

```
#domain
CLIENT_DOMAIN_URL=http://localhost:4200

#database connection
MYSQL_URL=jdbc:mysql://mysql:3308/alphaBeta?useSSL=false&useUnicode=yes&characterEncoding=UTF-8&allowPublicKeyRetrieval=true&serverTimezone=UTC
MYSQL_USERNAME=your_username
MYSQL_PASSWORD=your_pass

#stripe
STRIPE_API_KEY=
STRIPE_WEBHOOK_SECRET=

STRIPE_PRICE_STARTER_FLAT=price_...
STRIPE_PRICE_STARTER_USAGE=price_...

STRIPE_PRICE_PRO_FLAT=price_...
STRIPE_PRICE_PRO_USAGE=price_...

STRIPE_PRICE_BUSINESS_FLAT=price_...
STRIPE_PRICE_BUSINESS_USAGE=price_...

#admin
ADMIN_EMAIL=yourgmail@gmail.com
ADMIN_PASSWORD=password!

#email
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=yourgmail@gmail.com
SPRING_MAIL_PASSWORD=


#ai keys
OPENAI_API_KEY=
MISTRAL_API_KEY=


#use the online whisper from OpenAI API
USE_ONLINE=true

#where storage files go
STORAGE_ROOT="/files"

ENCRYPTION_KEY=ANY_String
```


# STRIPE TIERED USAGE
This application sets up 3 main tiered prices with the included amount a user can use. 
However, this application will also include billing if the user uses up more than they are allowed.


