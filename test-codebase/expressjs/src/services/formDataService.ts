import { Collection } from 'mongodb';
import { getDatabase } from '../database/connection';
import { FormDataSubmission, SavedFormDataSubmission, PaginatedResponse } from '../types';

export class FormDataService {
  private getCollection(): Collection<SavedFormDataSubmission> {
    return getDatabase().collection('form_submissions');
  }

  async saveFormData(
    formDataSubmission: FormDataSubmission,
    requestInfo: { ip?: string; userAgent?: string }
  ): Promise<{ isNewSubmission: boolean; submittedAt: string }> {
    const { formId, formTitle, formData, userInfo, submissionMetadata } = formDataSubmission;

    // Validate required fields
    if (!formId) {
      throw new Error('Form ID is required');
    }

    if (!formData || typeof formData !== 'object') {
      throw new Error('Invalid form data. Expected formData to be an object.');
    }

    // Prepare document for insertion
    const formDataDocument: SavedFormDataSubmission = {
      formId: formId,
      formTitle: formTitle,
      formData: formData,
      userInfo: userInfo,
      submissionMetadata: {
        submittedAt: new Date().toISOString(),
        ipAddress: requestInfo.ip || 'unknown',
        userAgent: requestInfo.userAgent || 'unknown',
        ...submissionMetadata
      },
      updatedAt: new Date().toISOString()
    };

    const collection = this.getCollection();
    
    // Insert new record (always creates a new document)
    const result = await collection.insertOne(formDataDocument);

    console.log(`Form data created successfully for form ID: ${formId} with insertion ID: ${result.insertedId}`);

    return {
      isNewSubmission: true, // Always true since we're always inserting
      submittedAt: formDataDocument.submissionMetadata.submittedAt
    };
  }

  async getFormData(formId: string, userId?: string): Promise<SavedFormDataSubmission | null> {
    const collection = this.getCollection();
    
    // Build filter
    const filter: any = { formId: formId };
    if (userId) {
      filter['userInfo.userId'] = userId;
    }

    return await collection.findOne(filter);
  }

  async getFormSubmissions(formId: string, page: number = 1, pageSize: number = 10, userId?: string): Promise<PaginatedResponse<SavedFormDataSubmission>> {
    const collection = this.getCollection();
    
    const skip = (page - 1) * pageSize;
    
    // Build filter
    const filter: any = { formId: formId };
    if (userId) {
      filter['userInfo.userId'] = userId;
    }
    
    // Get total count
    const totalCount = await collection.countDocuments(filter);
    
    // Get paginated submissions
    const submissions = await collection
      .find(filter)
      .sort({ 'submissionMetadata.submittedAt': -1 })
      .skip(skip)
      .limit(pageSize)
      .toArray();

    return {
      success: true,
      count: totalCount,
      page: page,
      pageSize: pageSize,
      totalPages: Math.ceil(totalCount / pageSize),
      data: submissions
    };
  }

  async getAllFormData(formId?: string, page: number = 1, pageSize: number = 10, userId?: string): Promise<PaginatedResponse<SavedFormDataSubmission>> {
    const collection = this.getCollection();
    
    const skip = (page - 1) * pageSize;
    
    // Build filter - if formId is provided, filter by it, if userId is provided, filter by it
    const filter: any = {};
    if (formId) {
      filter.formId = formId;
    }
    if (userId) {
      filter['userInfo.userId'] = userId;
    }
    
    // Get total count
    const totalCount = await collection.countDocuments(filter);
    
    // Get paginated submissions
    const submissions = await collection
      .find(filter)
      .sort({ 'submissionMetadata.submittedAt': -1 })
      .skip(skip)
      .limit(pageSize)
      .toArray();

    return {
      success: true,
      count: totalCount,
      page: page,
      pageSize: pageSize,
      totalPages: Math.ceil(totalCount / pageSize),
      data: submissions
    };
  }

  async deleteFormData(id: string): Promise<boolean> {
    const collection = this.getCollection();
    
    // Use MongoDB ObjectId for the deletion
    const { ObjectId } = require('mongodb');
    
    try {
      const result = await collection.deleteOne({ _id: new ObjectId(id) });
      
      if (result.deletedCount === 0) {
        console.log(`No form data found with ID: ${id}`);
        return false;
      }
      
      console.log(`Form data deleted successfully with ID: ${id}`);
      return true;
    } catch (error) {
      console.error('Error deleting form data:', error);
      throw new Error(`Failed to delete form data with ID: ${id}`);
    }
  }

  async searchFormData(searchQuery: string, page: number = 1, pageSize: number = 10, userId?: string): Promise<PaginatedResponse<SavedFormDataSubmission>> {
    const collection = this.getCollection();
    
    const skip = (page - 1) * pageSize;
    
    // Create search filter - search across form title, user info, and form field values
    let searchFilter: any = {};
    
    // Add user filter if provided
    if (userId) {
      searchFilter['userInfo.userId'] = userId;
    }
    
    if (searchQuery) {
      const basicSearchConditions = [
        { 'formTitle': { $regex: searchQuery, $options: 'i' } },
        { 'userInfo.submittedBy': { $regex: searchQuery, $options: 'i' } },
        { 'userInfo.username': { $regex: searchQuery, $options: 'i' } },
        { 'userInfo.email': { $regex: searchQuery, $options: 'i' } },
        { 'userInfo.name': { $regex: searchQuery, $options: 'i' } }
      ];

      // Add form data field searches
      const formDataSearchConditions = this.generateFormDataSearchConditions(searchQuery);
      
      const searchConditions = {
        $or: [...basicSearchConditions, ...formDataSearchConditions]
      };
      
      // Combine user filter and search filter
      if (userId) {
        searchFilter = {
          $and: [
            { 'userInfo.userId': userId },
            searchConditions
          ]
        };
      } else {
        searchFilter = searchConditions;
      }
    }
    
    // Get total count for search
    const totalCount = await collection.countDocuments(searchFilter);
    
    // Get paginated search results
    const formDataEntries = await collection
      .find(searchFilter)
      .sort({ 'submissionMetadata.submittedAt': -1 })
      .skip(skip)
      .limit(pageSize)
      .toArray();

    console.log(`Search results for query "${searchQuery}":`, formDataEntries.length, 'entries found.');
    console.log(formDataEntries);
    return {
      success: true,
      count: totalCount,
      page: page,
      pageSize: pageSize,
      totalPages: Math.ceil(totalCount / pageSize),
      data: formDataEntries
    };
  }

  private generateFormDataSearchConditions(searchQuery: string): any[] {
    const searchConditions = [];
    
    // Search in common form field patterns using dot notation
    const commonFields = [
      'name', 'email', 'phone', 'address', 'company', 'title', 
      'description', 'comment', 'message', 'notes', 'remarks',
      'givenName', 'familyName', 'firstName', 'lastName',
      'city', 'country', 'postcode', 'gender', 'programme',
      'studentNumber', 'mobileNo', 'guardianMobileNo'
    ];
    
    // Create search conditions for direct field access
    commonFields.forEach(field => {
      searchConditions.push({ [`formData.${field}.value`]: { $regex: searchQuery, $options: 'i' } });
      searchConditions.push({ [`formData.${field}`]: { $regex: searchQuery, $options: 'i' } });
      
      // Handle different field name variations (camelCase, with spaces, etc.)
      const variations = [
        field.toLowerCase(),
        field.charAt(0).toUpperCase() + field.slice(1),
        field.replace(/([A-Z])/g, ' $1').trim(),
        field.replace(/([a-z])([A-Z])/g, '$1 $2'),
        field.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase(),
        field.replace(/([a-z])([A-Z])/g, '$1_$2').toLowerCase()
      ];
      
      variations.forEach(variation => {
        searchConditions.push({ [`formData.${variation}.value`]: { $regex: searchQuery, $options: 'i' } });
        searchConditions.push({ [`formData.${variation}`]: { $regex: searchQuery, $options: 'i' } });
      });
    });
    
    // Search using aggregation for dynamic field matching
    searchConditions.push({
      $expr: {
        $gt: [
          {
            $size: {
              $filter: {
                input: { $objectToArray: "$formData" },
                cond: {
                  $or: [
                    // Search in field values that are strings
                    {
                      $and: [
                        { $eq: [{ $type: "$$this.v" }, "string"] },
                        { $regexMatch: { input: "$$this.v", regex: searchQuery, options: "i" } }
                      ]
                    },
                    // Search in nested value property for objects
                    {
                      $and: [
                        { $eq: [{ $type: "$$this.v" }, "object"] },
                        { $ne: ["$$this.v", null] },
                        { $eq: [{ $type: "$$this.v.value" }, "string"] },
                        { $regexMatch: { input: "$$this.v.value", regex: searchQuery, options: "i" } }
                      ]
                    }
                  ]
                }
              }
            }
          },
          0
        ]
      }
    });
    
    return searchConditions;
  }
}

export const formDataService = new FormDataService();