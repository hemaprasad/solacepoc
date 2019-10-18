package com.solace.connect;

import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class RestEndpointConnector {

	@Autowired
	BasicRequestor basicRequestor;

	@Value("${solace.queue}")
	private String queue;

	@RequestMapping(value = "/Publish", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject Columns(@RequestBody JSONObject request) throws Exception {
		JSONObject response = new JSONObject();

		String resp = basicRequestor.process(request.toString());
		response.put("result", resp);

		return response;
	}

}
