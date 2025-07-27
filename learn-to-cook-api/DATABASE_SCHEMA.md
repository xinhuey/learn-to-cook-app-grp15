# Database Schema Documentation

## Tables Overview

### users
- **id**: uuid (primary key, auto-generated)
- **full_name**: text (required)
- **email**: text (unique, required)
- **is_chef**: boolean (default: false)
- **bio**: text
- **location**: text
- **profile_image_url**: text
- **skill_level**: text (Easy, Medium, Hard)
- **food_preferences**: text[] (array of cuisine types)
- **dietary_restrictions**: text[] (array of dietary restrictions)
- **available_ingredients**: text[] (array of available ingredients)
- **languages_spoken**: text[] (array of language codes)
- **teaching_modes**: text[] (array of teaching mode preferences)
- **created_at**: timestamp (auto-generated)
- **updated_at**: timestamp (auto-updated)

### recipes
- **id**: uuid (primary key, auto-generated)
- **title**: text (required)
- **description**: text
- **ingredients**: text[] (required, array of ingredients)
- **instructions**: text[] (required, array of step-by-step instructions)
- **cuisine**: text (required, e.g., "Italian", "Chinese", "Mexican")
- **difficulty**: text (required, "Easy", "Medium", "Hard")
- **prep_time**: integer (preparation time in minutes)
- **cook_time**: integer (cooking time in minutes)
- **servings**: integer (number of servings)
- **image_urls**: text[] (array of recipe image URLs)
- **tags**: text[] (array of recipe tags/keywords)
- **author_id**: uuid (foreign key to users.id, required)
- **is_public**: boolean (default: true)
- **created_at**: timestamp (auto-generated)
- **updated_at**: timestamp (auto-updated)

### recipe_reviews
- **id**: uuid (primary key, auto-generated)
- **recipe_id**: uuid (foreign key to recipes.id, required)
- **user_id**: uuid (foreign key to users.id, required)
- **rating**: integer (required, 1-5 scale)
- **comment**: text (required)
- **created_at**: timestamp (auto-generated)
- **updated_at**: timestamp (auto-updated)

### user_follows
- **id**: uuid (primary key, auto-generated)
- **follower_id**: uuid (foreign key to users.id, required)
- **following_id**: uuid (foreign key to users.id, required)
- **created_at**: timestamp (auto-generated)

### notifications
- **id**: uuid (primary key, auto-generated)
- **user_id**: uuid (foreign key to users.id, required)
- **type**: text (required, e.g., "new_recipe", "new_follower", "recipe_review")
- **data**: jsonb (notification-specific data)
- **read**: boolean (default: false)
- **created_at**: timestamp (auto-generated)

## Relationships

### users → recipes
- One-to-many: A user can create multiple recipes
- Foreign key: recipes.author_id → users.id

### recipes → recipe_reviews
- One-to-many: A recipe can have multiple reviews
- Foreign key: recipe_reviews.recipe_id → recipes.id

### users → recipe_reviews
- One-to-many: A user can write multiple reviews
- Foreign key: recipe_reviews.user_id → users.id

### users → user_follows (follower)
- One-to-many: A user can follow multiple users
- Foreign key: user_follows.follower_id → users.id

### users → user_follows (following)
- One-to-many: A user can be followed by multiple users
- Foreign key: user_follows.following_id → users.id

### users → notifications
- One-to-many: A user can receive multiple notifications
- Foreign key: notifications.user_id → users.id

## Indexes for Performance

### recipes table
- Index on author_id for user recipe queries
- Index on cuisine for filtering
- Index on difficulty for filtering
- Index on created_at for ordering
- Composite index on (cuisine, difficulty) for combined filtering
- GIN index on ingredients for ingredient-based searches
- GIN index on tags for tag-based searches

### recipe_reviews table
- Index on recipe_id for recipe review queries
- Index on user_id for user review queries
- Composite index on (recipe_id, created_at) for ordered recipe reviews

### user_follows table
- Index on follower_id for follower queries
- Index on following_id for following queries
- Unique constraint on (follower_id, following_id) to prevent duplicate follows

### notifications table
- Index on user_id for user notification queries
- Index on read status for unread notification queries
- Composite index on (user_id, read, created_at) for efficient notification fetching

## SQL Creation Scripts

```sql
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create recipes table
CREATE TABLE recipes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title TEXT NOT NULL,
    description TEXT,
    ingredients TEXT[] NOT NULL,
    instructions TEXT[] NOT NULL,
    cuisine TEXT NOT NULL,
    difficulty TEXT NOT NULL CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
    prep_time INTEGER,
    cook_time INTEGER,
    servings INTEGER,
    image_urls TEXT[],
    tags TEXT[],
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_public BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create recipe_reviews table
CREATE TABLE recipe_reviews (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    recipe_id UUID NOT NULL REFERENCES recipes(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(recipe_id, user_id)
);

-- Create user_follows table
CREATE TABLE user_follows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    follower_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    following_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(follower_id, following_id),
    CHECK (follower_id != following_id)
);

-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    data JSONB,
    read BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for performance
CREATE INDEX idx_recipes_author_id ON recipes(author_id);
CREATE INDEX idx_recipes_cuisine ON recipes(cuisine);
CREATE INDEX idx_recipes_difficulty ON recipes(difficulty);
CREATE INDEX idx_recipes_created_at ON recipes(created_at DESC);
CREATE INDEX idx_recipes_cuisine_difficulty ON recipes(cuisine, difficulty);
CREATE INDEX idx_recipes_ingredients_gin ON recipes USING GIN(ingredients);
CREATE INDEX idx_recipes_tags_gin ON recipes USING GIN(tags);

CREATE INDEX idx_recipe_reviews_recipe_id ON recipe_reviews(recipe_id);
CREATE INDEX idx_recipe_reviews_user_id ON recipe_reviews(user_id);
CREATE INDEX idx_recipe_reviews_recipe_created ON recipe_reviews(recipe_id, created_at DESC);

CREATE INDEX idx_user_follows_follower_id ON user_follows(follower_id);
CREATE INDEX idx_user_follows_following_id ON user_follows(following_id);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_read ON notifications(read);
CREATE INDEX idx_notifications_user_read_created ON notifications(user_id, read, created_at DESC);

-- Create trigger for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_recipes_updated_at BEFORE UPDATE ON recipes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_recipe_reviews_updated_at BEFORE UPDATE ON recipe_reviews
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

## Data Validation Rules

### recipes table
- title: Required, non-empty string
- ingredients: Required, non-empty array
- instructions: Required, non-empty array
- cuisine: Required, predefined list of cuisines
- difficulty: Required, must be "Easy", "Medium", or "Hard"
- author_id: Required, must reference existing user

### recipe_reviews table
- rating: Required, integer between 1-5
- comment: Required, non-empty string
- Unique constraint: one review per user per recipe

### user_follows table
- Unique constraint: prevent duplicate follows
- Check constraint: users cannot follow themselves 