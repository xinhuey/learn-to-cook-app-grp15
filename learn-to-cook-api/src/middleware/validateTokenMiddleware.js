export default async function validateTokenMiddleware(c, next) {
    const authHeader = c.req.header('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return c.text('Authorization token missing or malformed', 401);
    }
    
    const uid = authHeader.substring(7).trim(); // remove "Bearer " from the start
    
    if (!uid || uid.length === 0) {
        return c.text('Invalid user ID', 401);
    }
    
    console.log("Authenticated user ID:", uid);
    c.set("uid", uid);
    return next();
} 