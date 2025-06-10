import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://nrnqgtupxhaxfpvcpcvy.supabase.co'
const supabaseKey = process.env.SUPABASE_KEY // TODO: Pass this in from context env
const supabase = createClient(supabaseUrl, supabaseKey)

/**
Users Table Schema
Table: users
id: uuid (primary key, auto-generated)
full_name: text (required)
email: text (unique, required)
is_chef: boolean (default: false)
bio: text
location: text
profile_image_url: text
skill_level: text
food_preferences: text[]
dietary_restrictions: text[]
available_ingredients: text[]
languages_spoken: text[]
teaching_modes: text[]
created_at: timestamp
updated_at: timestamp
*/

export const userHelpers = {

    //TODO: Add validation for user data + comments for the user schema for each helper function
  async createUser(userData) {
    try {
      const { data, error } = await supabase
        .from('users')
        .insert([userData])
        .select()
        .single()

      if (error) throw error
      return { success: true, data }
    } catch (error) {
      return { success: false, error: error.message }
    }
  },

  async getUserById(userId) {
    try {
      const { data, error } = await supabase
        .from('users')
        .select('*')
        .eq('id', userId)
        .single()

      if (error) throw error
      return { success: true, data }
    } catch (error) {
      return { success: false, error: error.message }
    }
  },

  async getUserByEmail(email) {
    try {
      const { data, error } = await supabase
        .from('users')
        .select('*')
        .eq('email', email)
        .single()

      if (error) throw error
      return { success: true, data }
    } catch (error) {
      return { success: false, error: error.message }
    }
  },

  async updateUser(userId, updates) {
    try {
      const updateData = {
        ...updates,
        updated_at: new Date().toISOString()
      }

      const { data, error } = await supabase
        .from('users')
        .update(updateData)
        .eq('id', userId)
        .select()
        .single()

      if (error) throw error
      return { success: true, data }
    } catch (error) {
      return { success: false, error: error.message }
    }
  },

  async deleteUser(userId) {
    try {
      const { error } = await supabase
        .from('users')
        .delete()
        .eq('id', userId)

      if (error) throw error
      return { success: true, message: 'User deleted successfully' }
    } catch (error) {
      return { success: false, error: error.message }
    }
  },
}

export default supabase