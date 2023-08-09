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
	 *            payload to store
	 * @return Response text from fedora
	 */
	void createRecord(String recordId, String recordXml);

	/**
	 * Stores a new binary in Fedora
	 * 
	 * @param binaryId
	 *            It is the identifier of the binary.
	 * @param binary
	 *            It is the binary file to store.
	 * @param binaryContentType
	 *            It is the content type of the binary to store.
	 */
	void createBinary(String recordId, InputStream binary, String binaryContentType);

	/**
	 * Reads a record from fedora using recordId
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
	 * Reads binary from fedora using recorid as identifier.
	 * <p>
	 * If a record with the specified recordId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 *
	 * @param recordId
	 *            It is the identifier of the binary to be read.
	 * @return InputStrem Representation of the binary read from Fedora
	 */
	InputStream readBinary(String recordId);

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
	 * Updates an existing binary in Fedora. The payload will update the binary with a new version
	 * in Fedora.
	 * <p>
	 * If a record with the same recordId is not already stored in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 * If there are problems while updating the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param recordId
	 *            identifier of the record to update
	 * @param binary
	 *            It is the binary file to update
	 * @param binaryContentType
	 *            It is the content type of the binary to update
	 */
	void updateBinary(String recordId, InputStream binary, String binaryContentType);

	/**
	 * Delete an existing record or binary in Fedora.
	 * 
	 * If a record or binary with the same recordId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * 
	 * If there are problems while updating the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param recordId
	 *            identifier of the record or binary to delete
	 */
	void delete(String recordId);
}
