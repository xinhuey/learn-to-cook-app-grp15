import { Hono } from 'hono'
import { createSupabaseClient, createUserHelpers } from './supabase.js'
import validateTokenMiddleware from './middleware/validateTokenMiddleware.js'
import {
  getRecipes,
  getRecipeById,
  createRecipe,
  updateRecipe,
  deleteRecipe,
  getRecommendedRecipes,
  getFeedRecipes,
  addRecipeReview,
  getRecipeReviews,
  followUser,
  unfollowUser,
  getUserFollowers,
  getUserRecipes,
  getAvailableCuisines,
  getRecipeCategories
} from './controllers/recipes.js'

const app = new Hono()

app.get('/', (c) => {
  return c.text('Learn to Cook API is running.')
})

// Helper function to generate UID from email using Web Crypto API
async function generateUserId(email) {
  const encoder = new TextEncoder()
  const data = encoder.encode(email)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('').substring(0, 32)
}

// Helper function to hash password using Web Crypto API
async function hashPassword(password) {
  const encoder = new TextEncoder()
  const data = encoder.encode(password)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

// Helper function to verify password
async function verifyPassword(password, hashedPassword) {
  const hashedInput = await hashPassword(password)
  return hashedInput === hashedPassword
}

// auth routes - these don't require authentication
app.post('/api/auth/register', async (c) => {
  try {
    const userData = await c.req.json()
    
    // basic validation
    if (!userData.email || !userData.password || !userData.full_name) {
      return c.json({ error: 'Email, password, and full name are required' }, 400)
    }

    if (userData.password.length < 6) {
      return c.json({ error: 'Password must be at least 6 characters long' }, 400)
    }

    // generate UID from email
    const userId = await generateUserId(userData.email)
    userData.id = userId

    // hash password before storing
    userData.password_hash = await hashPassword(userData.password)
    delete userData.password // don't store plain password

    // initialize supabase client with environment
    const supabase = createSupabaseClient(c.env)
    const userHelpers = createUserHelpers(supabase)

    // check if user already exists
    const existingUser = await userHelpers.getUserByEmail(userData.email)
    if (existingUser.success) {
      return c.json({ error: 'User already exists with this email' }, 409)
    }

    // create new user
    console.log('Creating user with data:', { ...userData, password_hash: '[HIDDEN]' })
    const result = await userHelpers.createUser(userData)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    // Return user data without password hash
    const userResponse = { ...result.data }
    delete userResponse.password_hash
    return c.json(userResponse, 201)
  } catch (error) {
    console.error('Registration error:', error)
    return c.json({ error: 'Invalid JSON data' }, 400)
  }
})

app.post('/api/auth/login', async (c) => {
  try {
    const { email, password } = await c.req.json()
    
    if (!email || !password) {
      return c.json({ error: 'Email and password are required' }, 400)
    }

    const supabase = createSupabaseClient(c.env)
    const userHelpers = createUserHelpers(supabase)

    // get user by email (including password hash)
    const result = await userHelpers.getUserByEmail(email)
    
    if (!result.success) {
      return c.json({ error: 'Invalid email or password' }, 401)
    }

    // verify password
    const isValidPassword = await verifyPassword(password, result.data.password_hash)
    if (!isValidPassword) {
      return c.json({ error: 'Invalid email or password' }, 401)
    }
    
    // Return user data without password hash
    const userResponse = { ...result.data }
    delete userResponse.password_hash
    return c.json(userResponse)
  } catch (error) {
    console.error('Login error:', error)
    return c.json({ error: 'Invalid JSON data' }, 400)
  }
})

// apply authentication middleware to all protected routes
app.use('/api/users/*', validateTokenMiddleware)

// protected routes (require user is authd)
app.get('/api/users/:id', async (c) => {
  const userId = c.req.param('id')
  
  const supabase = createSupabaseClient(c.env)
  const userHelpers = createUserHelpers(supabase)
  
  const result = await userHelpers.getUserById(userId)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  // Don't return password hash
  const userResponse = { ...result.data }
  delete userResponse.password_hash
  return c.json(userResponse)
})

app.put('/api/users/:id', async (c) => {
  try {
    const userId = c.req.param('id')
    const authenticatedUid = c.get('uid')
    
    // users can only update their own profile
    if (authenticatedUid !== userId) {
      return c.json({ error: 'Unauthorized: You can only update your own profile' }, 403)
    }
    
    const updates = await c.req.json()
    
    // don't allow updating certain fields
    delete updates.id
    delete updates.created_at
    delete updates.password_hash // don't allow direct password hash updates
    
    // If password is being updated, hash it
    if (updates.password) {
      updates.password_hash = await hashPassword(updates.password)
      delete updates.password
    }
    
    const supabase = createSupabaseClient(c.env)
    const userHelpers = createUserHelpers(supabase)
    
    const result = await userHelpers.updateUser(userId, updates)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    // Don't return password hash
    const userResponse = { ...result.data }
    delete userResponse.password_hash
    return c.json(userResponse)
  } catch (error) {
    return c.json({ error: 'Invalid JSON data' }, 400)
  }
})

app.delete('/api/users/:id', async (c) => {
  const userId = c.req.param('id')
  const authenticatedUid = c.get('uid')
  
  if (authenticatedUid !== userId) {
    return c.json({ error: 'Unauthorized: You can only delete your own account' }, 403)
  }
  
  const supabase = createSupabaseClient(c.env)
  const userHelpers = createUserHelpers(supabase)
  
  const result = await userHelpers.deleteUser(userId)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  return c.json({ message: result.message })
})

// get user
app.get('/api/users/me', async (c) => {
  const authenticatedUid = c.get('uid')
  
  const supabase = createSupabaseClient(c.env)
  const userHelpers = createUserHelpers(supabase)
  
  const result = await userHelpers.getUserById(authenticatedUid)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  // Don't return password hash
  const userResponse = { ...result.data }
  delete userResponse.password_hash
  return c.json(userResponse)
})

app.use('/api/recipes/*', validateTokenMiddleware)
app.use('/api/feed/*', validateTokenMiddleware)
app.use('/api/recommendations/*', validateTokenMiddleware)
app.use('/api/follow/*', validateTokenMiddleware)

app.get('/api/recipes/cuisines', getAvailableCuisines)
app.get('/api/recipes/categories', getRecipeCategories)
app.get('/api/recipes', getRecipes)
app.get('/api/recipes/:id', getRecipeById)
app.post('/api/recipes', createRecipe)
app.put('/api/recipes/:id', updateRecipe)
app.delete('/api/recipes/:id', deleteRecipe)

app.get('/api/recommendations/recipes', getRecommendedRecipes)
app.get('/api/feed/recipes', getFeedRecipes)

app.post('/api/recipes/:id/reviews', addRecipeReview)
app.get('/api/recipes/:id/reviews', getRecipeReviews)

app.post('/api/follow/:userId', followUser)
app.delete('/api/follow/:userId', unfollowUser)
app.get('/api/users/:userId/followers', getUserFollowers)
app.get('/api/users/:userId/recipes', getUserRecipes)

export default app