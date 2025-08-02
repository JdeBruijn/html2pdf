
rm -rf classes/

mkdir classes

jars="$(find -iname "*.jar" | tr '\n' ':' | sed 's/\.\///g')"

echo "jars=$jars"

javac -cp "$jars." -d classes/ src/*.java

if [ $? -gt 0 ]
then
	echo "Failed to compile"
	exit 1
fi

java -cp "$jars.:./classes/"  HtmlToPdfConverter $1 $2
