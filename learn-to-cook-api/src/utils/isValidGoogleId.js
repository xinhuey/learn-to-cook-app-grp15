import * as jose from 'jose';

async function getFirebaseKeys() {
    const response = await fetch("https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com");
    return await response.json();
}


export default async function isValidGoogleId(jwt, env = {}) {
    try {
        console.info("decoding protected header")
        const header = jose.decodeProtectedHeader(jwt);
        console.info("decoding jwt")
        const data = jose.decodeJwt(jwt);
        
        const projectId = env.PROJECT_ID;
        if (data.iss !== `https://securetoken.google.com/${projectId}`) {
            console.log(`jwt not valid - expected https://securetoken.google.com/${projectId} got, ${data.iss}`);
            return false;
        }
        
        console.info("getting firebase keys")
        const firebaseKeys = await getFirebaseKeys();
        if (!firebaseKeys[header.kid]) {
            console.log("jwt not valid - signing key not in firebase keys")
            return false;
        }
        
        console.info("importing firebase key to jose")
        const key = await jose.importX509(firebaseKeys[header.kid], "RS256");
        await jose.jwtVerify(jwt, key);
        console.info("jwt is valid")
        return true;
    } catch (err) {
        console.log("isValidGoogleID failed with error", err);
        return false;
    }
} 