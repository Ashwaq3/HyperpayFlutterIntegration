const functions = require("firebase-functions");
const https = require('https');
const querystring = require('querystring');


exports.checkoutId = functions.https.onCall((data1, _) => {
   	const path='/v1/checkouts';
   	const data = querystring.stringify({
   		'entityId'://todo,
   		'amount':'92.00',
   		'currency':'SAR',
   		'paymentType':'DB'
   	});
   	const options = {
   		port: 443,
   		host: 'eu-test.oppwa.com',
   		path: path,
   		method: 'POST',
   		headers: {
   			'Content-Type': 'application/x-www-form-urlencoded',
   			'Content-Length': data.length,
   			'Authorization'://todo
   		}
   	};
   	return new Promise((resolve, reject) => {
   		const postRequest = https.request(options, function(res) {
   			const buf = [];
   			res.on('data', chunk => {
   				buf.push(Buffer.from(chunk));
   			});
   			res.on('end', () => {
   				const jsonString = Buffer.concat(buf).toString('utf8');
   				try {
   					resolve(JSON.parse(jsonString));
   				} catch (error) {
   					reject(error);
   				}
   			});
   		});
   		postRequest.on('error', reject);
   		postRequest.write(data);
   		postRequest.end();
   	});
});

exports.paymentStatus = functions.https.onCall((data1, _) => {
var path='/v1/checkouts/'+ data1.toString() + '/payment';
	path += '?entityId=' //todo;
	const options = {
		port: 443,
		host: 'eu-test.oppwa.com',
		path: path,
		method: 'GET',
		headers: {
			'Authorization'://todo
		}
	};
	return new Promise((resolve, reject) => {
		const postRequest = https.request(options, function(res) {
			const buf = [];
			res.on('data', chunk => {
				buf.push(Buffer.from(chunk));
			});
			res.on('end', () => {
				const jsonString = Buffer.concat(buf).toString('utf8');
				try {
					resolve(JSON.parse(jsonString));
				} catch (error) {
					reject(error);
				}
			});
		});
		postRequest.on('error', reject);
		postRequest.end();
	});
});

