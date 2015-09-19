package py.fpuna.lcca.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.web.AbstractFilter;

@Startup
@Scope(ScopeType.APPLICATION)
@Name("richFacesFirefox11Filter")
@BypassInterceptors
@Filter
public class RichFacesFirefox11Filter extends AbstractFilter {
 
     public void doFilter(final ServletRequest request, final ServletResponse response,
               final FilterChain chain) throws IOException, ServletException {
          chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest) request) {
 
               public String getRequestURI() {
                try {
                    return URLDecoder.decode(super.getRequestURI(), "ISO-8859-1");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException("Cannot decode request URI.", e);
                }
            }
        }, response);
    }
 
    public void init(FilterConfig filterConfig) throws ServletException {
        // do nothing
    }
 
    public void destroy() {
        // do nothing
    }
}

