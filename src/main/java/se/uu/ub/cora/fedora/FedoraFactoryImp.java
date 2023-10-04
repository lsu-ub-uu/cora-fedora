/*
 * Copyright 2022, 2023 Uppsala University Library
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

import se.uu.ub.cora.fedora.internal.FedoraAdapterImp;
import se.uu.ub.cora.fedora.internal.ResourceMetadataParser;
import se.uu.ub.cora.fedora.internal.ResourceMetadataParserImp;
import se.uu.ub.cora.httphandler.HttpHandlerFactoryImp;
import se.uu.ub.cora.json.parser.org.OrgJsonParser;

public class FedoraFactoryImp implements FedoraFactory {

	private HttpHandlerFactoryImp httpHandlerFactory;
	private String fedoraUrl;

	public FedoraFactoryImp(String fedoraUrl) {
		this.fedoraUrl = fedoraUrl;
		httpHandlerFactory = new HttpHandlerFactoryImp();
	}

	@Override
	public FedoraAdapter factorFedoraAdapter() {
		OrgJsonParser orgJsonParser = new OrgJsonParser();
		ResourceMetadataParser parser = ResourceMetadataParserImp.usingJsonParser(orgJsonParser);
		return new FedoraAdapterImp(httpHandlerFactory, fedoraUrl, parser);
	}

	public String onlyForTestGetBaseUrl() {
		return fedoraUrl;
	}

}
