/*
 * Copyright 2022 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.fedora;

import se.uu.ub.cora.httphandler.HttpHandler;
import se.uu.ub.cora.httphandler.HttpHandlerFactory;

public class FedoraImp implements Fedora {

	private HttpHandlerFactory httpHandlerFactory;
	private String baseUrl;

	public FedoraImp(HttpHandlerFactory httpHandlerFactory, String baseUrl) {
		this.httpHandlerFactory = httpHandlerFactory;
		this.baseUrl = baseUrl;
	}

	@Override
	public String create(String recordId, String fedoraXML) {
		// TODO: kolla att post finns inte innan vi skapar den. Annars för vi 204, vad betyder att
		// den post har skapats som en version på en annan post. Det vill vi inte i create.
		HttpHandler httpHandler = httpHandlerFactory.factor(baseUrl + recordId);
		httpHandler.setRequestMethod("PUT");
		httpHandler.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
		try {
			httpHandler.setOutput(fedoraXML);
		} catch (RuntimeException e) {
			// TODO: handle exception
			throw FedoraException.withMessage("Error connecting to fedora, with url: " + baseUrl);
		}

		throwErrorIfCreateNotOk(httpHandler, recordId);
		return httpHandler.getResponseText();

	}

	private void throwErrorIfCreateNotOk(HttpHandler httpHandler, String recordId) {
		int responseCode = httpHandler.getResponseCode();
		if (responseCode != 201) {
			throw FedoraException
					.withMessage("Error storing record in Fedora, recordId: " + recordId);
		}
	}

	@Override
	public String read(String recordId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String update(String recordId, String fedoraXML) {
		// TODO Auto-generated method stub
		return null;
	}

}
