import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://nrnqgtupxhaxfpvcpcvy.supabase.co'

// Create a function to initialize Supabase client with environment
export function createSupabaseClient(env) {
  const supabaseKey = env.SUPABASE_KEY
  return createClient(supabaseUrl, supabaseKey)
}

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

export function createUserHelpers(supabase) {
  return {
    //TODO: Add validation for user data + comments for the user schema for each helper function
  async createUser(userData) {
    try {
      const { data, error } = await supabase
        .from('users')
        .insert([userData])
        .select()
        .single()

        console.log(data)
        console.log(error)
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

  async getUserByIdWithFollowerCount(userId) {
    try {
      // Get user data
      const { data: userData, error: userError } = await supabase
        .from('users')
        .select('*')
        .eq('id', userId)
        .single()

      if (userError) throw userError

      // Get follower count
      const { count: followerCount, error: countError } = await supabase
        .from('user_follows')
        .select('*', { count: 'exact', head: true })
        .eq('following_id', userId)

      if (countError) throw countError

      // Add follower count to user data
      const userWithFollowerCount = {
        ...userData,
        follower_count: followerCount || 0
      }

      return { success: true, data: userWithFollowerCount }
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
}

export const userHelpers = {
  createUser: (userData) => { throw new Error('Use createUserHelpers with proper environment context') },
  getUserById: (userId) => { throw new Error('Use createUserHelpers with proper environment context') },
  getUserByEmail: (email) => { throw new Error('Use createUserHelpers with proper environment context') },
  updateUser: (userId, updates) => { throw new Error('Use createUserHelpers with proper environment context') },
  deleteUser: (userId) => { throw new Error('Use createUserHelpers with proper environment context') }
}

export function createRecipeHelpers(supabase) {
  return {
    async searchRecipes(filters) {
      try {
        let query = supabase
          .from('recipes')
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url),
            recipe_reviews(rating)
          `)
          .order('created_at', { ascending: false })

        if (filters.cuisine) {
          query = query.eq('cuisine', filters.cuisine)
        }

        if (filters.difficulty) {
          query = query.eq('difficulty', filters.difficulty)
        }

        if (filters.search) {
          const searchTerms = filters.search.toLowerCase().split(/[,\s]+/).filter(term => term.trim().length > 0)
          
          if (searchTerms.length > 0) {
            const searchConditions = searchTerms.map(term => 
              `title.ilike.%${term}%,description.ilike.%${term}%,ingredients.cs.{${term}}`
            ).join(',')
            
            query = query.or(searchConditions)
          }
        }

        if (filters.ingredients) {
          const preferredIngredients = filters.ingredients.split(',').map(ing => ing.trim()).filter(ing => ing.length > 0)
          if (preferredIngredients.length > 0) {
            query = query.overlaps('ingredients', preferredIngredients)
          }
        }

        try {
          if (filters.excludeIngredients) {
            console.log("Processing excludeIngredients:", filters.excludeIngredients);

            const allergyIngredients = filters.excludeIngredients
              .split(',')
              .map(ing => ing.trim())
              .filter(ing => ing.length > 0);

            console.log("Parsed allergy ingredients:", allergyIngredients);

            if (allergyIngredients.length > 0) {
              const pgArrayString = `{${allergyIngredients.join(',')}}`;
              console.log("Adding exclude condition with overlaps:", pgArrayString);

              query = query.not('allergies_ingredients', 'ov', pgArrayString);
            }
          }
        } catch (error) {
          console.log("Error in excludeIngredients:", error)
        }
        

        const offset = (filters.page - 1) * filters.limit
        query = query.range(offset, offset + filters.limit - 1)

        const { data, error } = await query

        if (error) throw error

        const recipesWithRatings = data.map(recipe => ({
          ...recipe,
          average_rating: recipe.recipe_reviews.length > 0 
            ? recipe.recipe_reviews.reduce((sum, review) => sum + review.rating, 0) / recipe.recipe_reviews.length
            : 0,
          review_count: recipe.recipe_reviews.length
        }))

        return { success: true, data: recipesWithRatings }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getRecipeDetails(recipeId) {
      try {
        const { data, error } = await supabase
          .from('recipes')
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url, bio),
            recipe_reviews(
              id,
              rating,
              comment,
              created_at,
              user:users!recipe_reviews_user_id_fkey(id, full_name, profile_image_url)
            )
          `)
          .eq('id', recipeId)
          .single()

        if (error) throw error

        const average_rating = data.recipe_reviews.length > 0 
          ? data.recipe_reviews.reduce((sum, review) => sum + review.rating, 0) / data.recipe_reviews.length
          : 0

        return { 
          success: true, 
          data: {
            ...data,
            average_rating,
            review_count: data.recipe_reviews.length
          }
        }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getRecipeById(recipeId) {
      try {
        const { data, error } = await supabase
          .from('recipes')
          .select('*')
          .eq('id', recipeId)
          .single()

        if (error) throw error
        return { success: true, data }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async insertRecipe(recipeData) {
      try {
        const { data, error } = await supabase
          .from('recipes')
          .insert([recipeData])
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url)
          `)
          .single()

        if (error) throw error
        return { success: true, data }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async updateRecipe(recipeId, updates) {
      try {
        const updateData = {
          ...updates,
          updated_at: new Date().toISOString()
        }

        const { data, error } = await supabase
          .from('recipes')
          .update(updateData)
          .eq('id', recipeId)
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url)
          `)
          .single()

        if (error) throw error
        return { success: true, data }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async deleteRecipe(recipeId) {
      try {
        const { error } = await supabase
          .from('recipes')
          .delete()
          .eq('id', recipeId)

        if (error) throw error
        return { success: true, message: 'Recipe deleted successfully' }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getRecommendedRecipes(user, limit) {
      try {
        let query = supabase
          .from('recipes')
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url),
            recipe_reviews(rating)
          `)

        if (user.food_preferences && user.food_preferences.length > 0) {
          query = query.in('cuisine', user.food_preferences)
        }

        if (user.skill_level) {
          const skillLevels = ['Easy', 'Medium', 'Hard']
          const userSkillIndex = skillLevels.indexOf(user.skill_level)
          if (userSkillIndex !== -1) {
            const allowedLevels = skillLevels.slice(0, userSkillIndex + 1)
            query = query.in('difficulty', allowedLevels)
          }
        }

        if (user.available_ingredients && user.available_ingredients.length > 0) {
          query = query.overlaps('ingredients', user.available_ingredients)
        }

        query = query
          .neq('author_id', user.id)
          .order('created_at', { ascending: false })
          .limit(limit)

        const { data, error } = await query

        if (error) throw error

        const recipesWithRatings = data.map(recipe => ({
          ...recipe,
          average_rating: recipe.recipe_reviews.length > 0 
            ? recipe.recipe_reviews.reduce((sum, review) => sum + review.rating, 0) / recipe.recipe_reviews.length
            : 0
        }))

        return { success: true, data: recipesWithRatings }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getFeedRecipes(userId, page, limit) {
      try {
        const offset = (page - 1) * limit

        const { data, error } = await supabase
          .from('recipes')
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url),
            recipe_reviews(rating)
          `)
          .in('author_id', supabase
            .from('user_follows')
            .select('following_id')
            .eq('follower_id', userId)
          )
          .order('created_at', { ascending: false })
          .range(offset, offset + limit - 1)

        if (error) throw error

        const recipesWithRatings = data.map(recipe => ({
          ...recipe,
          average_rating: recipe.recipe_reviews.length > 0 
            ? recipe.recipe_reviews.reduce((sum, review) => sum + review.rating, 0) / recipe.recipe_reviews.length
            : 0
        }))

        return { success: true, data: recipesWithRatings }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getFeedRecipesWithFilters(userId, filters) {
      try {
        
        // First, get the list of users that the current user follows
        const { data: followingData, error: followingError } = await supabase
          .from('user_follows')
          .select('following_id')
          .eq('follower_id', userId)

        if (followingError) {
          console.error('Error getting following IDs:', followingError)
          throw followingError
        }

        
        if (!followingData || followingData.length === 0) {
          return { success: true, data: [] }
        }

        // Extract the following IDs
        const followingIds = followingData.map(item => item.following_id)
        
        let query = supabase
          .from('recipes')
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url),
            recipe_reviews(rating)
          `)
          .in('author_id', followingIds)


        // Apply filters
        if (filters.cuisine) {
          query = query.eq('cuisine', filters.cuisine)
        }

        if (filters.difficulty) {
          query = query.eq('difficulty', filters.difficulty)
        }

        if (filters.search) {
          const searchTerms = filters.search.toLowerCase().split(/[,\s]+/).filter(term => term.trim().length > 0)
          
          if (searchTerms.length > 0) {
            const searchConditions = searchTerms.map(term => 
              `title.ilike.%${term}%,description.ilike.%${term}%,ingredients.cs.{${term}}`
            ).join(',')
            
            query = query.or(searchConditions)
          }
        }

        if (filters.ingredients) {
          const preferredIngredients = filters.ingredients.split(',').map(ing => ing.trim()).filter(ing => ing.length > 0)
          if (preferredIngredients.length > 0) {
            query = query.overlaps('ingredients', preferredIngredients)
          }
        }

        if (filters.excludeIngredients) {
          const allergyIngredients = filters.excludeIngredients
            .split(',')
            .map(ing => ing.trim())
            .filter(ing => ing.length > 0)

          if (allergyIngredients.length > 0) {
            const pgArrayString = `{${allergyIngredients.join(',')}}`
            query = query.not('allergies_ingredients', 'ov', pgArrayString)
          }
        }

        const offset = (filters.page - 1) * filters.limit
        
        query = query
          .order('created_at', { ascending: false })
          .range(offset, offset + filters.limit - 1)

        const { data, error } = await query

        if (error) {
          console.error('Query error:', error)
          throw error
        }


        if (!Array.isArray(data)) {
          console.error('Data is not an array:', typeof data, data)
          throw new Error('Expected array of recipes but got: ' + typeof data)
        }

        const recipesWithRatings = data.map((recipe, index) => {
          
          if (!recipe) {
            return null
          }

          if (!recipe.recipe_reviews) {
            return {
              ...recipe,
              average_rating: 0,
              review_count: 0
            }
          }

          if (!Array.isArray(recipe.recipe_reviews)) {
            return {
              ...recipe,
              average_rating: 0,
              review_count: 0
            }
          }

          const average_rating = recipe.recipe_reviews.length > 0 
            ? recipe.recipe_reviews.reduce((sum, review) => {
                return sum + (review?.rating || 0)
              }, 0) / recipe.recipe_reviews.length
            : 0


          return {
            ...recipe,
            average_rating,
            review_count: recipe.recipe_reviews.length
          }
        }).filter(recipe => recipe !== null)

        return { success: true, data: recipesWithRatings }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async uploadRecipeImage(file, filename){
        try{
            const path = `public/${filename}`

            const {data, error} = await supabase
                .storage
                .from('recipe-images')
                .upload(path, file)

                if(error) throw error

                const{ data : urlData} = supabase
                    .storage
                    .from('recipe-images')
                    .getPublicUrl(data.path)
                return {success: true, data: urlData.publicUrl}

        } catch(error){
        return {success: false, error: error.message}
        }
    },

    async insertReview(reviewData) {
      try {
        const { data, error } = await supabase
          .from('recipe_reviews')
          .insert([reviewData])
          .select(`
            *,
            user:users!recipe_reviews_user_id_fkey(id, full_name, profile_image_url)
          `)
          .single()

        if (error) throw error
        return { success: true, data }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getRecipeReviews(recipeId, page, limit) {
      try {
        const offset = (page - 1) * limit

        const { data, error } = await supabase
          .from('recipe_reviews')
          .select(`
            *,
            user:users!recipe_reviews_user_id_fkey(id, full_name, profile_image_url)
          `)
          .eq('recipe_id', recipeId)
          .order('created_at', { ascending: false })
          .range(offset, offset + limit - 1)

        if (error) throw error
        return { success: true, data }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async followUser(followerId, followingId) {
      try {
        const { data, error } = await supabase
          .from('user_follows')
          .insert([{ 
            follower_id: followerId, 
            following_id: followingId 
          }])
          .select()
          .single()

        if (error) throw error
        return { success: true, data }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async unfollowUser(followerId, followingId) {
      try {
        const { error } = await supabase
          .from('user_follows')
          .delete()
          .eq('follower_id', followerId)
          .eq('following_id', followingId)

        if (error) throw error
        return { success: true, message: 'Successfully unfollowed user' }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getFollowers(userId, page, limit) {
      try {
        const offset = (page - 1) * limit

        const { data, error } = await supabase
          .from('user_follows')
          .select(`
            follower:users!user_follows_follower_id_fkey(id, full_name, profile_image_url, bio)
          `)
          .eq('following_id', userId)
          .range(offset, offset + limit - 1)

        if (error) throw error
        return { success: true, data: data.map(item => item.follower) }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async isUserFollowing(followerId, followingId) {
      try {
        const { data, error } = await supabase
          .from('user_follows')
          .select('id')
          .eq('follower_id', followerId)
          .eq('following_id', followingId)
          .single()

        if (error && error.code !== 'PGRST116') { // PGRST116 is "not found" error
          throw error
        }
        
        return { success: true, data: { isFollowing: !!data } }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getFollowerCount(userId) {
      try {
        const { count, error } = await supabase
          .from('user_follows')
          .select('*', { count: 'exact', head: true })
          .eq('following_id', userId)

        if (error) throw error
        return { success: true, data: { followerCount: count || 0 } }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getAllChefs(page, limit) {
      try {
        const offset = (page - 1) * limit
        const { data: chefs, error } = await supabase
          .from('users')
          .select('id, full_name, profile_image_url, bio, skill_level, specialty, chef_expertise, is_chef')
          .eq('is_chef', true)
          .range(offset, offset + limit - 1)
        if (error) throw error
        const chefsWithFollowers = await Promise.all(
          chefs.map(async (chef) => {
            const followerResult = await this.getFollowerCount(chef.id)
            return {
              ...chef,
              follower_count: followerResult.success ? followerResult.data.followerCount : 0
            }
          })
        )
        return { success: true, data: chefsWithFollowers }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },
    async searchChefs(searchQuery, page, limit) {
      try {
        const offset = (page - 1) * limit
        const { data: chefs, error } = await supabase
          .from('users')
          .select('id, full_name, profile_image_url, bio, skill_level, specialty, chef_expertise, is_chef')
          .eq('is_chef', true)
          .or(`full_name.ilike.%${searchQuery}%,bio.ilike.%${searchQuery}%,specialty.ilike.%${searchQuery}%,chef_expertise.ilike.%${searchQuery}%`)
          .range(offset, offset + limit - 1)
        if (error) throw error
        const chefsWithFollowers = await Promise.all(
          chefs.map(async (chef) => {
            const followerResult = await this.getFollowerCount(chef.id)
            return {
              ...chef,
              follower_count: followerResult.success ? followerResult.data.followerCount : 0
            }
          })
        )
        return { success: true, data: chefsWithFollowers }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getUserRecipes(userId, page, limit) {
      try {
        const offset = (page - 1) * limit

        const { data, error } = await supabase
          .from('recipes')
          .select(`
            *,
            author:users!recipes_author_id_fkey(id, full_name, profile_image_url),
            recipe_reviews(rating)
          `)
          .eq('author_id', userId)
          .order('created_at', { ascending: false })
          .range(offset, offset + limit - 1)

        if (error) throw error

        const recipesWithRatings = data.map(recipe => ({
          ...recipe,
          average_rating: recipe.recipe_reviews.length > 0 
            ? recipe.recipe_reviews.reduce((sum, review) => sum + review.rating, 0) / recipe.recipe_reviews.length
            : 0
        }))

        return { success: true, data: recipesWithRatings }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async notifyFollowers(recipeId, authorId) {
      try {
        const { data: followers, error } = await supabase
          .from('user_follows')
          .select('follower_id')
          .eq('following_id', authorId)

        if (error) throw error

        const notifications = followers.map(follower => ({
          user_id: follower.follower_id,
          type: 'new_recipe',
          data: { recipe_id: recipeId, author_id: authorId },
          created_at: new Date().toISOString()
        }))

        if (notifications.length > 0) {
          await supabase
            .from('notifications')
            .insert(notifications)
        }

        return { success: true }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getAvailableCuisines() {
      try {
        const predefinedCuisines = [
          'American',
          'Chinese',
          'French',
          'Greek',
          'Indian',
          'Italian',
          'Japanese',
          'Korean',
          'Mediterranean',
          'Mexican',
          'Middle Eastern',
          'Spanish',
          'Thai',
          'Vietnamese'
        ]

        return { success: true, data: predefinedCuisines }
      } catch (error) {
        return { success: false, error: error.message }
      }
    },

    async getRecipeCategories() {
      try {
        const predefinedCuisines = [
          'American',
          'Chinese',
          'French',
          'Greek',
          'Indian',
          'Italian',
          'Japanese',
          'Korean',
          'Mediterranean',
          'Mexican',
          'Middle Eastern',
          'Spanish',
          'Thai',
          'Vietnamese'
        ]

        const difficultyLevels = ['Easy', 'Medium', 'Hard']

        const { data: tagsData, error: tagsError } = await supabase
          .from('recipes')
          .select('tags')
          .not('tags', 'is', null)

        if (tagsError) throw tagsError

        const allTags = tagsData
          .flatMap(recipe => recipe.tags || [])
          .filter(tag => tag && tag.trim())
        const uniqueTags = [...new Set(allTags)].sort()

        return {
          success: true,
          data: {
            cuisines: predefinedCuisines,
            difficulties: difficultyLevels,
            tags: uniqueTags
          }
        }
      } catch (error) {
        return { success: false, error: error.message }
      }
    }
  }
}