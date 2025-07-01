import { Request, Response } from 'express';
import { formService } from '../services';
import { SaveFormRequest } from '../types';

export class FormController {
  async saveForm(req: Request, res: Response): Promise<void> {
    try {
      const formRequest: SaveFormRequest = req.body;
      const userId = req.user?.userId; // Get user ID from auth middleware

      if (!userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }

      const result = await formService.saveForm(formRequest, userId);

      res.status(200).json({
        success: true,
        message: 'Form saved successfully',
        data: {
          formId: result.formId,
          savedAt: result.savedAt
        }
      });

    } catch (error: any) {
      console.error('Error saving form:', error);
      res.status(400).json({ 
        success: false,
        error: 'Failed to save form', 
        message: error.message 
      });
    }
  }

  async getForms(req: Request, res: Response): Promise<void> {
    try {
      const page = parseInt(req.query.page as string) || 1;
      const pageSize = parseInt(req.query.pageSize as string) || 10;
      const userId = req.user?.userId; // Get user ID from auth middleware

      if (!userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }
      
      const result = await formService.getForms(page, pageSize, userId);

      res.status(200).json({
        success: result.success,
        count: result.count,
        page: result.page,
        pageSize: result.pageSize,
        totalPages: result.totalPages,
        data: result.data
      });

    } catch (error: any) {
      console.error('Error retrieving forms:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to retrieve forms', 
        message: error.message 
      });
    }
  }

  async getFormById(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const userId = req.user?.userId; // Get user ID from auth middleware

      if (!userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }

      const form = await formService.getFormById(id, userId);

      if (!form) {
        res.status(404).json({
          success: false,
          error: 'Form not found',
          message: `No form found with ID: ${id} or you don't have permission to access it`
        });
        return;
      }

      res.status(200).json({
        success: true,
        form: form
      });

    } catch (error: any) {
      console.error('Error retrieving form:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to retrieve form', 
        message: error.message 
      });
    }
  }

  async searchForms(req: Request, res: Response): Promise<void> {
    try {
      const searchQuery = req.query.search as string || '';
      const page = parseInt(req.query.page as string) || 1;
      const pageSize = parseInt(req.query.pageSize as string) || 10;
      const userId = req.user?.userId; // Get user ID from auth middleware

      if (!userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }
      
      const result = await formService.searchForms(searchQuery, page, pageSize, userId);

      res.status(200).json({
        success: result.success,
        count: result.count,
        page: result.page,
        pageSize: result.pageSize,
        totalPages: result.totalPages,
        forms: result.data,
        searchQuery: searchQuery
      });

    } catch (error: any) {
      console.error('Error searching forms:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to search forms', 
        message: error.message 
      });
    }
  }

  async deleteForm(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const userId = req.user?.userId; // Get user ID from auth middleware

      if (!userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }

      const deleted = await formService.deleteForm(id, userId);

      if (!deleted) {
        res.status(404).json({
          success: false,
          error: 'Form not found',
          message: `No form found with ID: ${id} or you don't have permission to delete it`
        });
        return;
      }

      res.status(200).json({
        success: true,
        message: 'Form deleted successfully'
      });

    } catch (error: any) {
      console.error('Error deleting form:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to delete form', 
        message: error.message 
      });
    }
  }

  async updateForm(req: Request, res: Response): Promise<void> {
    try {
      const { id } = req.params;
      const formData = req.body;
      const user = req.user; // Get user from auth middleware

      if (!user || !user.userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }

      // Extract complete user information for tracking
      const userInfo = {
        userId: user.userId,
        username: user.username,
        userFullName: user.fullName
      };
      
      const updatedForm = await formService.updateForm(id, formData, user.userId, userInfo);

      if (!updatedForm) {
        res.status(404).json({
          success: false,
          error: 'Form not found',
          message: `No form found with ID: ${id} or you don't have permission to update it`
        });
        return;
      }

      res.status(200).json({
        success: true,
        message: 'Form updated successfully',
        form: updatedForm
      });

    } catch (error: any) {
      console.error('Error updating form:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to update form', 
        message: error.message 
      });
    }
  }

  async getFormsByPdfFingerprint(req: Request, res: Response): Promise<void> {
    try {
      const { fingerprint } = req.params;
      const userId = req.user?.userId; // Get user ID from auth middleware

      if (!userId) {
        res.status(401).json({
          success: false,
          error: 'Authentication required',
          message: 'User ID not found in request'
        });
        return;
      }

      if (!fingerprint) {
        res.status(400).json({
          success: false,
          error: 'PDF fingerprint required',
          message: 'Fingerprint parameter is missing'
        });
        return;
      }

      const forms = await formService.getFormsByPdfFingerprint(fingerprint, userId);

      res.status(200).json({
        success: true,
        message: `Found ${forms.length} forms with PDF fingerprint: ${fingerprint}`,
        count: forms.length,
        data: forms
      });

    } catch (error: any) {
      console.error('Error retrieving forms by PDF fingerprint:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to retrieve forms by PDF fingerprint', 
        message: error.message 
      });
    }
  }

  async verifyForm(req: Request, res: Response): Promise<void> {
    try {
      const { formId } = req.params;

      // Validate MongoDB ObjectId format (24 hex characters)
      if (!formId || !/^[0-9a-fA-F]{24}$/.test(formId)) {
        res.status(400).json({
          success: false,
          error: 'Invalid form ID format.'
        });
        return;
      }

      const result = await formService.verifyFormStatus(formId);

      if (!result.success) {
        res.status(404).json(result);
        return;
      }

      res.status(200).json(result);

    } catch (error: any) {
      console.error('Error verifying form:', error);
      res.status(500).json({ 
        success: false,
        error: 'Failed to verify form', 
        message: error.message 
      });
    }
  }
}

export const formController = new FormController();