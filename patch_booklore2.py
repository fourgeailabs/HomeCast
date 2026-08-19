import re

file_path = "app/src/main/java/com/example/data/network/BookloreClient.kt"
with open(file_path, "r") as f:
    text = f.read()

# Let's fix URL formation. If the user uses port 6060, the hostUrl might be `http://192.168.1.50:6060`. 
# Wait, "Booklore" is apparently Kavita or something else, but there are multiple paths?
# Ah, if the server returns HTTP 200 but parsing fails, we should check what Booklore returns.
# Let's add multiple endpoint checks just in case they aren't using `/api/v1/library/books`.

