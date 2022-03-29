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

/**
 * FedoraFactory is a factory interface that provides instances of classes in the fedora module.
 * <p>
 * To be able to fullfill this interface must implementing factories be supplied with the baseUrl to
 * fedora, how this is done is up to the implementing classes to decide.
 * <p>
 * Implementations of FedoraFactory MUST be threadsafe.
 */
public interface FedoraFactory {

	/**
	 * factorFedoraAdapter creates and returns a new instance of FedoraAdapter. The returned
	 * FedoraAdapter SHOULD by the implementing factory be set up with connection details (baseUrl)
	 * needed to call the Fedora Rest server.
	 * 
	 * @return A FedoraAdapter set up with connection details for a fedora.
	 */
	public FedoraAdapter factorFedoraAdapter();

}
