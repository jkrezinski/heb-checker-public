# heb-checker-public
HEB curbside check

Simple tool that will text you HEB curbside availability based on your zip code / configuration.

## Requirements

1. Java 11
2. Maven
3. AWS Simple Notification Service (http://aws.amazon.com/sns)

SNS is used to send yourself text messages and be notified. You could modify the code to just print to console output and run it locally though.

## Steps

1. Clone project
2. Replace https://github.com/jkrezinski/heb-checker-public/blob/master/src/main/java/Main.java#L42-L47 with your configuration
3. Run `mvn clean install`
4. Run `chmod +x start.sh`
4. Run `./start.sh`
