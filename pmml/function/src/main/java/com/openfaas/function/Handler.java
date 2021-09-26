package com.openfaas.function;


import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import com.openfaas.model.IResponse;
import com.openfaas.model.IRequest;
import com.openfaas.model.Response;

import org.pmml4s.model.Model;
import org.json.JSONException;
import org.json.JSONObject;
public class Handler extends com.openfaas.model.AbstractHandler {
		static Model model;
    public IResponse Handle(IRequest req) {
        Response res = new Response();
        String req_json_str = req.getBody();
        JSONObject res_json = new JSONObject();
        int statuscode = 500;
        try {
            res_json.put("success", false);
            res_json.put("message", "Error scoring model file");
            JSONObject req_json = new JSONObject(req_json_str);
            JSONObject output_json = pmml4sscore(req_json);
						System.out.println("Scoring completed...");

						//Thread.sleep(10000);
            if(output_json != null) {
                res_json = output_json;
                if(output_json.getBoolean("success")) {
                    statuscode = 200;
                }
            }

        } catch(OutOfMemoryError e) {
						//trigger unhealthy
						try {
							Files.deleteIfExists(Paths.get("/tmp/.lock"));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
				} catch (JSONException e) {
            e.printStackTrace();
            res_json = prepareResponse("An internal server error has occured.", false);
        } finally {
            res.setBody(res_json.toString());
            res.setStatusCode(statuscode);
        }
			System.out.println("Returning now...");
	    return res;
    }

    public JSONObject pmml4sscore(JSONObject input) {
			try {
				if (model == null) {
					InputStream inputStream = getClass().getResourceAsStream("/assets/model.file");
					if(inputStream != null) {
						model = Model.fromInputStream(inputStream);
					} else {
						JSONObject err = prepareResponse("Unable to load model", false);
						return err;
					}
				} else {
					System.out.println("Model already loaded in memory...");
				}

				String[] input_arr = new String[input.names().length()];
				for(int i = 0; i<input.names().length(); i++){
					input_arr[i] = input.get(input.names().getString(i)).toString();
				}
				System.out.println(Arrays.toString(input_arr));
				Object[] result = model.predict(input_arr);
				JSONObject op = new JSONObject();
				op.put("output", result);
				op.put("success", true);
					
				System.out.println(op);
				return op;
			} catch (Exception e) {
				System.out.println(e.getMessage());
				JSONObject err = prepareResponse(e.getMessage(), false);
				return err;
			}
    }
    
	public JSONObject prepareResponse(String message, boolean success) {
		JSONObject err = new JSONObject();
		try {
			err.put("success", success);
			err.put("message", message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return err;
	}
}
