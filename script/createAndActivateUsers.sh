for i in `seq 0 200`
do
    USERNAME="scriptuser$i"
    PASSWORD=$(./createUser.sh "$i" "$USERNAME" "hsh.helper%2Bscriptuser$i%40gmail.com" "400")
    echo $PASSWORD
    ./activateUser.sh $i "$USERNAME" "$PASSWORD" "scriptuser123"
done
