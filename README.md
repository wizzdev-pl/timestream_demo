# Timestream

Project is a showcase of new AWS service - Timestream Database. 
Our main goal is to unpack data from HDF5 file and send them to the Timestream. 

Another feature is a REST API for providing local path to HDF5 file. Reading and sending are taken automatically after sending a post request to SPRING. 

## Requirements

All required libraries are listed in pom.xml file. However, some of them aren't available in the maven repository. 
1. We are using the newest version of jarhdf5 lib.
2. In case the AWS Timestream Service is available only as a preview, there is no other way to gather SDK than signing up for a preview. 

To run this demo, you must have an AWS account. 