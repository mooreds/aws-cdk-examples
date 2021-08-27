
const AWS = require('aws-sdk');
const S3 = new AWS.S3();


const bucketName = process.env.BUCKET;


exports.main = async function(event, context) {
  try {
    var method = event.httpMethod;

    if (method === "POST") {
    
      var webhookEvent = {};
      try {
        webhookEvent = JSON.parse(event.body);
      } catch (e) {
        var body = error.stack || JSON.stringify(error, null, 2);
        return {
          statusCode: 400,
       	  headers: {},
          body: body
        }
      }
        
      if (!webhookEvent.event.id) {
        return {
          statusCode: 400,
       	  headers: {},
          body: "Unable to parse event"
        }
      }  
      
      const created = new Date(webhookEvent.event.createInstant);
      const prefix = created.getFullYear() + "/" + (created.getMonth() + 1) + "/" + created.getDate();
      const base64data = new Buffer(event.body, 'binary');

      await S3.putObject({
        Bucket: bucketName,
        Key: prefix+"/"+webhookEvent.event.id,
        Body: base64data,
        ContentType: 'application/json'
      }).promise();

      return {
        statusCode: 200,
        headers: {},
        body: ""
      };
    }


    // We got something besides a GET, POST, or DELETE
    return {
      statusCode: 400,
      headers: {},
      body: "We only accept POST not " + method
    };
  } catch(error) {
    var body = error.stack || JSON.stringify(error, null, 2);
    return {
      statusCode: 400,
      headers: {},
      body: body
    }
  }
}