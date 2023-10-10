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

import se.uu.ub.cora.fedora.record.ResourceMetadata;
import se.uu.ub.cora.fedora.record.ResourceMetadataToUpdate;

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
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param recordId
	 *            identifier of the record to store
	 * @param recordXml
	 *            payload to store in XML
	 * 
	 * @return Response text from fedora
	 */
	void createRecord(String dataDivider, String recordId, String recordXml);

	/**
	 * Stores a new resource in Fedora
	 * <p>
	 * If a record with the same recordId is already stored in fedora a
	 * {@link FedoraConflictException} will be thrown.
	 * <p>
	 * For any other problem a {@link FedoraException} will be thrown.
	 * 
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param resourceId
	 *            It is the identifier of the resource.
	 * @param resource
	 *            It is the resource file to store.
	 * @param mimeType
	 *            It is the mimeType of the resource to store.
	 */
	void createResource(String dataDivider, String resourceId, InputStream resource,
			String mimeType);

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
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param recordId
	 *            identifies the record to read
	 * 
	 * @return record fecthed from fedora
	 */
	String readRecord(String dataDivider, String recordId);

	/**
	 * Reads resource from fedora using recorid as identifier.
	 * <p>
	 * If the resource with the specified resourceId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown.
	 * <p>
	 * 
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param resourceId
	 *            It is the identifier of the resource to be read.
	 *
	 * @return InputStrem Representation of the resource read from Fedora
	 */
	InputStream readResource(String dataDivider, String resourceId);

	/**
	 * Reads a resource from fedora using resourceId as identifier.
	 * <p>
	 * If the resource with the specified resourceId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown. *
	 * <p>
	 * If there are problems while reading the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param resourceId
	 *            It is the identifier of the resource to be read.
	 *
	 * @return InputStrem Representation of the resource read from Fedora
	 */
	ResourceMetadata readResourceMetadata(String dataDivider, String resourceId);

	/**
	 * Updates resource metadata from fedora using resourceId and datadivider. It updates all
	 * metadata specified in ResourceMetadataToUpdate.
	 * <p>
	 * If the resource with the specified resourceId is not found in fedora a
	 * {@link FedoraNotFoundException} will be thrown. *
	 * <p>
	 * If there are problems while reading the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param dataDivider
	 *            It is the name of the data divider where the record belongs
	 * @param resourceId
	 *            It is the identifier of the resource to be read.
	 * @param ResourceMetadataToUpdate
	 *            A record containing the metadata use to update resource metadata in fedora.
	 */
	void updateResourceMetadata(String dataDivider, String resourceId,
			ResourceMetadataToUpdate resourceMetadataToUpdate);

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
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param recordId
	 *            identifier of the record to update
	 * @param recordXml
	 *            payload to update
	 */
	void updateRecord(String dataDivider, String recordId, String recordXml);

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
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param resourceId
	 *            identifier of the resource to update
	 * @param resource
	 *            It is the resource to update
	 * @param mimeType
	 *            It is the mimeType of the resource to update
	 */
	void updateResource(String dataDivider, String resourceId, InputStream resource,
			String mimeType);

	/**
	 * Delete an existing record in Fedora.
	 * 
	 * If a record with the same id is not found in fedora a {@link FedoraNotFoundException} will be
	 * thrown.
	 * 
	 * If there are problems while updating the record in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param dataDivider
	 *            it is the name of the data divider where the record belongs
	 * @param recordId
	 *            identifier of the record to delete
	 */
	void deleteRecord(String dataDivider, String recordId);

	/**
	 * Delete an existing reosurce in Fedora.
	 * 
	 * If a resource with the same id is not found in fedora a {@link FedoraNotFoundException} will
	 * be thrown.
	 * 
	 * If there are problems while updating the resource in Fedora a {@link FedoraException} will be
	 * thrown.
	 * 
	 * @param dataDivider
	 *            it is the name of the data divider where the resource belongs
	 * @param resourceId
	 *            identifier of the resource to delete
	 */
	void deleteResource(String dataDivider, String resourceId);
}
