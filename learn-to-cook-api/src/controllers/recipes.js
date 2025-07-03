import { createSupabaseClient, createRecipeHelpers, createUserHelpers } from '../supabase.js'

export const getRecipes = async (c) => {
  try {
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const cuisine = c.req.query('cuisine')
    const difficulty = c.req.query('difficulty')
    const search = c.req.query('search')
    const page = parseInt(c.req.query('page')) || 1
    const limit = parseInt(c.req.query('limit')) || 20
    
    const filters = { cuisine, difficulty, search, page, limit }
    const result = await recipeHelpers.searchRecipes(filters)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }

    console.log(result.data)
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch recipes' }, 500)
  }
}

export const getRecipeById = async (c) => {
  try {
    const recipeId = c.req.param('id')
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getRecipeDetails(recipeId)
    
    if (!result.success) {
      return c.json({ error: result.error }, 404)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch recipe details' }, 500)
  }
}

export const createRecipe = async (c) => {
  try {
    const authenticatedUid = c.get('uid')
    const recipeData = await c.req.json()
    
    if (!recipeData.title || !recipeData.ingredients || !recipeData.instructions) {
      return c.json({ error: 'Title, ingredients, and instructions are required' }, 400)
    }

    const supabase = createSupabaseClient(c.env)
    const userHelpers = createUserHelpers(supabase)
    
    const userResult = await userHelpers.getUserById(authenticatedUid)
    if (!userResult.success) {
      return c.json({ error: 'User not found' }, 404)
    }
    
    if (!userResult.data.is_chef) {
      return c.json({ error: 'Only chefs can create recipes' }, 403)
    }

    const validCuisines = [
      'American', 'Chinese', 'French', 'Greek', 'Indian', 'Italian',
      'Japanese', 'Korean', 'Mediterranean', 'Mexican', 'Middle Eastern',
      'Spanish', 'Thai', 'Vietnamese'
    ]
    
    const validDifficulties = ['Easy', 'Medium', 'Hard']
    
    if (recipeData.cuisine && !validCuisines.includes(recipeData.cuisine)) {
      return c.json({ error: 'Invalid cuisine. Must be one of: ' + validCuisines.join(', ') }, 400)
    }
    
    if (recipeData.difficulty && !validDifficulties.includes(recipeData.difficulty)) {
      return c.json({ error: 'Invalid difficulty. Must be one of: ' + validDifficulties.join(', ') }, 400)
    }
    
    recipeData.author_id = authenticatedUid
    
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.insertRecipe(recipeData)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    await recipeHelpers.notifyFollowers(result.data.id, authenticatedUid)
    
    return c.json(result.data, 201)
  } catch (error) {
    return c.json({ error: 'Invalid JSON data or failed to create recipe' }, 400)
  }
}

export const updateRecipe = async (c) => {
  try {
    const recipeId = c.req.param('id')
    const authenticatedUid = c.get('uid')
    const updates = await c.req.json()
    
    const supabase = createSupabaseClient(c.env)
    const userHelpers = createUserHelpers(supabase)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const userResult = await userHelpers.getUserById(authenticatedUid)
    if (!userResult.success) {
      return c.json({ error: 'User not found' }, 404)
    }
    
    if (!userResult.data.is_chef) {
      return c.json({ error: 'Only chefs can update recipes' }, 403)
    }
    
    const validCuisines = [
      'American', 'Chinese', 'French', 'Greek', 'Indian', 'Italian',
      'Japanese', 'Korean', 'Mediterranean', 'Mexican', 'Middle Eastern',
      'Spanish', 'Thai', 'Vietnamese'
    ]
    
    const validDifficulties = ['Easy', 'Medium', 'Hard']
    
    if (updates.cuisine && !validCuisines.includes(updates.cuisine)) {
      return c.json({ error: 'Invalid cuisine. Must be one of: ' + validCuisines.join(', ') }, 400)
    }
    
    if (updates.difficulty && !validDifficulties.includes(updates.difficulty)) {
      return c.json({ error: 'Invalid difficulty. Must be one of: ' + validDifficulties.join(', ') }, 400)
    }
    
    const existingRecipe = await recipeHelpers.getRecipeById(recipeId)
    if (!existingRecipe.success) {
      return c.json({ error: 'Recipe not found' }, 404)
    }
    
    if (existingRecipe.data.author_id !== authenticatedUid) {
      return c.json({ error: 'Unauthorized: You can only update your own recipes' }, 403)
    }
    
    delete updates.id
    delete updates.author_id
    delete updates.created_at
    
    const result = await recipeHelpers.updateRecipe(recipeId, updates)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Invalid JSON data or failed to update recipe' }, 400)
  }
}

export const deleteRecipe = async (c) => {
  try {
    const recipeId = c.req.param('id')
    const authenticatedUid = c.get('uid')
    
    const supabase = createSupabaseClient(c.env)
    const userHelpers = createUserHelpers(supabase)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const userResult = await userHelpers.getUserById(authenticatedUid)
    if (!userResult.success) {
      return c.json({ error: 'User not found' }, 404)
    }
    
    if (!userResult.data.is_chef) {
      return c.json({ error: 'Only chefs can delete recipes' }, 403)
    }
    
    const existingRecipe = await recipeHelpers.getRecipeById(recipeId)
    if (!existingRecipe.success) {
      return c.json({ error: 'Recipe not found' }, 404)
    }
    
    if (existingRecipe.data.author_id !== authenticatedUid) {
      return c.json({ error: 'Unauthorized: You can only delete your own recipes' }, 403)
    }
    
    const result = await recipeHelpers.deleteRecipe(recipeId)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json({ message: 'Recipe deleted successfully' })
  } catch (error) {
    return c.json({ error: 'Failed to delete recipe' }, 500)
  }
}

export const getRecommendedRecipes = async (c) => {
  try {
    const authenticatedUid = c.get('uid')
    const limit = parseInt(c.req.query('limit')) || 10
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    const userHelpers = createUserHelpers(supabase)
    
    const userResult = await userHelpers.getUserById(authenticatedUid)
    if (!userResult.success) {
      return c.json({ error: 'User not found' }, 404)
    }
    
    const result = await recipeHelpers.getRecommendedRecipes(userResult.data, limit)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch recommended recipes' }, 500)
  }
}

export const getFeedRecipes = async (c) => {
  try {
    const authenticatedUid = c.get('uid')
    const page = parseInt(c.req.query('page')) || 1
    const limit = parseInt(c.req.query('limit')) || 20
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getFeedRecipes(authenticatedUid, page, limit)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch feed recipes' }, 500)
  }
}

export const addRecipeReview = async (c) => {
  try {
    const recipeId = c.req.param('id')
    const authenticatedUid = c.get('uid')
    const reviewData = await c.req.json()
    
    if (!reviewData.rating || !reviewData.comment) {
      return c.json({ error: 'Rating and comment are required' }, 400)
    }
    
    reviewData.recipe_id = recipeId
    reviewData.user_id = authenticatedUid
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.insertReview(reviewData)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data, 201)
  } catch (error) {
    return c.json({ error: 'Invalid JSON data or failed to add review' }, 400)
  }
}

export const getRecipeReviews = async (c) => {
  try {
    const recipeId = c.req.param('id')
    const page = parseInt(c.req.query('page')) || 1
    const limit = parseInt(c.req.query('limit')) || 10
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getRecipeReviews(recipeId, page, limit)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch recipe reviews' }, 500)
  }
}

export const followUser = async (c) => {
  try {
    const targetUserId = c.req.param('userId')
    const authenticatedUid = c.get('uid')
    
    if (targetUserId === authenticatedUid) {
      return c.json({ error: 'Cannot follow yourself' }, 400)
    }
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.followUser(authenticatedUid, targetUserId)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json({ message: 'Successfully followed user' }, 201)
  } catch (error) {
    return c.json({ error: 'Failed to follow user' }, 500)
  }
}

export const unfollowUser = async (c) => {
  try {
    const targetUserId = c.req.param('userId')
    const authenticatedUid = c.get('uid')
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.unfollowUser(authenticatedUid, targetUserId)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json({ message: 'Successfully unfollowed user' })
  } catch (error) {
    return c.json({ error: 'Failed to unfollow user' }, 500)
  }
}

export const getUserFollowers = async (c) => {
  try {
    const userId = c.req.param('userId')
    const page = parseInt(c.req.query('page')) || 1
    const limit = parseInt(c.req.query('limit')) || 20
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getFollowers(userId, page, limit)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch followers' }, 500)
  }
}

export const getUserRecipes = async (c) => {
  try {
    const userId = c.req.param('userId')
    const page = parseInt(c.req.query('page')) || 1
    const limit = parseInt(c.req.query('limit')) || 20
    
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getUserRecipes(userId, page, limit)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch user recipes' }, 500)
  }
}

export const getAvailableCuisines = async (c) => {
  try {
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getAvailableCuisines()
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch available cuisines' }, 500)
  }
}

export const getRecipeCategories = async (c) => {
  try {
    const supabase = createSupabaseClient(c.env)
    const recipeHelpers = createRecipeHelpers(supabase)
    
    const result = await recipeHelpers.getRecipeCategories()
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Failed to fetch recipe categories' }, 500)
  }
} 