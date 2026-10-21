package ht.treechop.common.config.resource;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SingleResourceIdentifier extends ResourceIdentifier {

    public SingleResourceIdentifier(String nameSpace, String localSpace, List<IdentifierQualifier> qualifiers, String string) {
        super(nameSpace, localSpace, qualifiers, string);
    }

    @Override
    public <R extends DefaultedRegistry<T>, T> Stream<T> resolve(R registry) {
        String resourceString = getNamespace() + ":" + getLocalSpace();
        Identifier key = Identifier.tryParse(resourceString);
        if (key != null) {
            if (registry.containsKey(key)) {
                Optional<Holder.Reference<T>> optionalResourceRef = registry.get(key);
                T resource = optionalResourceRef.map(Holder.Reference::value).orElse(null);

                Identifier defaultKey = registry.getDefaultKey();
                if (!registry.getKey(resource).equals(defaultKey) || key.equals(defaultKey)) {
                    return Stream.of(resource);
                }
            }
        } else {
            parsingError(String.format("\"%s\" is not a valid resource location", getIdentifier()));
        }

        return Stream.empty();
    }

}
