import { Hono } from 'hono'
import { userHelpers } from './supabase.js'
import validateTokenMiddleware from './middleware/validateTokenMiddleware.js'

const app = new Hono()

app.get('/', (c) => {
  return c.text('Learn to Cook API is running.')
})

// auth routes - these don't require authentication
app.post('/api/auth/register', async (c) => {
  try {
    const userData = await c.req.json()
    
    // basic validation
    if (!userData.email || !userData.full_name) {
      return c.json({ error: 'Email and full name are required' }, 400)
    }

    // check if user already exists
    const existingUser = await userHelpers.getUserByEmail(userData.email)
    if (existingUser.success) {
      return c.json({ error: 'User already exists with this email' }, 409)
    }

    // create new user
    const result = await userHelpers.createUser(userData)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data, 201)
  } catch (error) {
    return c.json({ error: 'Invalid JSON data' }, 400)
  }
})

app.post('/api/auth/login', async (c) => {
  try {
    const { email } = await c.req.json()
    
    if (!email) {
      return c.json({ error: 'Email is required' }, 400)
    }

    // get user by email
    const result = await userHelpers.getUserByEmail(email)
    
    if (!result.success) {
      return c.json({ error: 'User not found' }, 404)
    }
    
    return c.json(result.data)
  } catch (error) {
    return c.json({ error: 'Invalid JSON data' }, 400)
  }
})

// apply authentication middleware to all protected routes
app.use('/api/users/*', validateTokenMiddleware)

// protected routes (require user is authd)
app.get('/api/users/:id', async (c) => {
  const userId = c.req.param('id') //might be able to just do c.get('uid') assuming our token logic works?
  const result = await userHelpers.getUserById(userId)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  return c.json(result.data)
})

app.get('/api/users/email/:email', async (c) => {
  const email = c.req.param('email')
  const result = await userHelpers.getUserByEmail(email)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  return c.json(result.data)
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
    
    const result = await userHelpers.updateUser(userId, updates)
    
    if (!result.success) {
      return c.json({ error: result.error }, 400)
    }
    
    return c.json(result.data)
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
  
  const result = await userHelpers.deleteUser(userId)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  return c.json({ message: result.message })
})

// get user
app.get('/api/users/me', async (c) => {
  const authenticatedUid = c.get('uid')
  
  const result = await userHelpers.getUserById(authenticatedUid)
  
  if (!result.success) {
    return c.json({ error: result.error }, 400)
  }
  
  return c.json(result.data)
})

export default app