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

import java.io.InputStream;

/**
 * FedoraAdapter defines an interface in order to talk to Fedora 6+ hiding the implementation
 * details.
 * <p>
 * Implementation are generally not expected to be thread safe.
 */

public interface FedoraAdapter {

	/**
	 * Creates a record in fedora using recordId as id and recordXml as payload
	 * <p>
	 * If a record with the same recordId is already stored in fedora a
	 * {@link FedoraConflictException} will be thrown.
	 * <p>
	 * For any other problem a {@link FedoraException} will be thrown.
	 * 
	 * @param recordId
	 *            identifier of the record to store
	 * @param recordXml
	 *            payload to store in XML
	 * @return Response text from fedora
	 */
	void createRecord(String recordId, String recordXml);

	/**
	 * Stores a new resource in Fedora
	 * 
	 * @param resourceId
	 *            It is the identifier of the resource.
	 * @param resource
	 *            It is the resource file to store.
	 * @param mimeType
	 *            It is the mimeType of the resource to store.
	 */
	void createResource(String resourceId, InputStream resource, String mimeType);

	/**
	 * Reads a record from fedora using recordId
	 * <p>
	 * <p>
	 * If a record with the specified recordId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 * If there are problems while reading the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param recordId
	 *            identifies the record to read
	 * @return record fecthed from fedora
	 */
	String readRecord(String recordId);

	/**
	 * Reads resource from fedora using recorid as identifier.
	 * <p>
	 * If the resource with the specified resourceId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 *
	 * @param resourceId
	 *            It is the identifier of the resource to be read.
	 * @return InputStrem Representation of the resource read from Fedora
	 */
	InputStream readResource(String resourceId);

	/**
	 * Updates an existing record in Fedora. The payload (recordXml) will create a new version of
	 * the record in Fedora.
	 * <p>
	 * If a record with the same recordId is not already stored in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 * If there are problems while updating the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 *
	 * @param recordId
	 *            identifier of the record to update
	 * @param recordXml
	 *            payload to update
	 */
	void updateRecord(String recordId, String recordXml);

	/**
	 * Updates an existing resource in Fedora. The payload will update the resource with a new
	 * version in Fedora.
	 * <p>
	 * If a resource with the same resourceId is not already stored in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 * If there are problems while updating the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param resourceId
	 *            identifier of the record to update
	 * @param resource
	 *            It is the resource to update
	 * @param mimeType
	 *            It is the mimeType of the resource to update
	 */
	void updateResource(String resourceId, InputStream resource, String mimeType);

	/**
	 * Delete an existing record or resource in Fedora.
	 * 
	 * If a record or resource with the same id is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * 
	 * If there are problems while updating the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param id
	 *            identifier of the record or resource to delete
	 */
	void delete(String id);
}
