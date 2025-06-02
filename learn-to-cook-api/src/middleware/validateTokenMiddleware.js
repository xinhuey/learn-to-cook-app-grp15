import * as jose from 'jose';
import isValidGoogleId from "../utils/isValidGoogleId.js";

export default async function validateTokenMiddleware(c, next) {
    const authHeader = c.req.header('Authorization');
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return c.text('Authorization token missing or malformed', 401);
    }
    const token = authHeader.substring(7).trim(); // remove "Bearer " from th start
    console.log(token);
    try {

        if (await isValidGoogleId(token, c.env)) {
            const payload = jose.decodeJwt(token);
            console.log("payload: ", payload)
            c.set("uid", payload.sub);
            return next();
        } else {
            return c.text('Invalid token', 401);
        }
    } catch (err) {
        console.error("validateTokenMiddleware failed with error", err);
        return c.text("Internal Server Error", 500);
    }
} 