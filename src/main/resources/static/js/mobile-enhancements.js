/**
 * Mobile Enhancement JavaScript for Kolors Application
 * Provides touch-optimized interactions and mobile-specific functionality
 */

document.addEventListener('DOMContentLoaded', function() {
    // Initialize mobile enhancements
    initTouchFeedback();
    initMobileFormOptimizations();
    initMobileColorManagement();
    initMobileSearchOptimizations();
    initOrientationHandling();
    initAccessibilityEnhancements();
});

/**
 * Add touch feedback to interactive elements
 */
function initTouchFeedback() {
    const interactiveElements = document.querySelectorAll('.btn, .remove-color-btn, .combination-card, .color-box, .palette-color');

    interactiveElements.forEach(element => {
        // Add touch start feedback
        element.addEventListener('touchstart', function(e) {
            this.classList.add('touch-active');

            // Add haptic feedback if available
            if (navigator.vibrate) {
                navigator.vibrate(10);
            }
        }, { passive: true });

        // Remove touch feedback
        element.addEventListener('touchend', function(e) {
            setTimeout(() => {
                this.classList.remove('touch-active');
            }, 150);
        }, { passive: true });

        // Handle touch cancel
        element.addEventListener('touchcancel', function(e) {
            this.classList.remove('touch-active');
        }, { passive: true });
    });
}

/**
 * Optimize forms for mobile devices
 */
function initMobileFormOptimizations() {
    // Prevent zoom on iOS when focusing inputs
    const inputs = document.querySelectorAll('input[type="text"], input[type="email"], input[type="password"], select, textarea');

    inputs.forEach(input => {
        // Ensure minimum font size to prevent zoom
        if (window.innerWidth <= 767) {
            const computedStyle = window.getComputedStyle(input);
            const fontSize = parseFloat(computedStyle.fontSize);
            if (fontSize < 16) {
                input.style.fontSize = '16px';
            }
        }

        // Add mobile-specific input handling
        input.addEventListener('focus', function() {
            // Scroll element into view on mobile
            if (window.innerWidth <= 767) {
                setTimeout(() => {
                    this.scrollIntoView({
                        behavior: 'smooth',
                        block: 'center'
                    });
                }, 300);
            }
        });
    });

    // Optimize color inputs specifically
    const colorInputs = document.querySelectorAll('.color-input');
    colorInputs.forEach(input => {
        // Add pattern and inputmode for better mobile keyboards
        input.setAttribute('pattern', '[0-9A-Fa-f]{6}');
        input.setAttribute('inputmode', 'text');
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('autocorrect', 'off');
        input.setAttribute('autocapitalize', 'characters');

        // Add mobile-specific validation feedback
        input.addEventListener('input', function(e) {
            const value = e.target.value.toUpperCase();
            const isValid = /^[0-9A-F]{0,6}$/.test(value);

            // Visual feedback for mobile users
            if (!isValid && value.length > 0) {
                this.style.borderColor = '#dc3545';
                this.style.boxShadow = '0 0 0 2px rgba(220, 53, 69, 0.25)';
            } else {
                this.style.borderColor = '';
                this.style.boxShadow = '';
            }
        });
    });
}

/**
 * Enhance color management for mobile devices
 */
function initMobileColorManagement() {
    // Improve color preview interactions
    const colorPreviews = document.querySelectorAll('.color-preview, .color-box, .combination-color');

    colorPreviews.forEach(preview => {
        // Add long press for color copying on mobile
        let longPressTimer;

        preview.addEventListener('touchstart', function(e) {
            longPressTimer = setTimeout(() => {
                const color = this.style.backgroundColor || this.dataset.hex;
                if (color) {
                    copyColorToClipboard(color);
                    showMobileToast('Color copiado: ' + color);
                }
            }, 500);
        }, { passive: true });

        preview.addEventListener('touchend', function(e) {
            clearTimeout(longPressTimer);
        }, { passive: true });

        preview.addEventListener('touchmove', function(e) {
            clearTimeout(longPressTimer);
        }, { passive: true });
    });

    // Optimize remove color buttons for touch
    const removeButtons = document.querySelectorAll('.remove-color-btn');
    removeButtons.forEach(button => {
        // Increase touch target size
        button.style.minWidth = '44px';
        button.style.minHeight = '44px';

        // Add confirmation for mobile
        button.addEventListener('click', function(e) {
            if (window.innerWidth <= 767) {
                e.preventDefault();
                if (confirm('¿Eliminar este color?')) {
                    // Proceed with removal
                    if (this.onclick) {
                        this.onclick();
                    }
                }
            }
        });
    });
}

/**
 * Optimize search functionality for mobile
 */
function initMobileSearchOptimizations() {
    const searchInputs = document.querySelectorAll('input[type="search"], input[name="search"]');

    searchInputs.forEach(input => {
        // Add mobile search enhancements
        input.setAttribute('autocomplete', 'off');
        input.setAttribute('autocorrect', 'off');
        input.setAttribute('autocapitalize', 'off');

        // Debounced search for mobile
        let searchTimeout;
        input.addEventListener('input', function(e) {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                // Trigger search after user stops typing
                const form = this.closest('form');
                if (form && window.innerWidth <= 767) {
                    // Auto-submit on mobile after delay
                    form.submit();
                }
            }, 800);
        });
    });
}

/**
 * Handle orientation changes
 */
function initOrientationHandling() {
    function handleOrientationChange() {
        // Adjust layout after orientation change
        setTimeout(() => {
            // Recalculate viewport height for mobile browsers
            const vh = window.innerHeight * 0.01;
            document.documentElement.style.setProperty('--vh', `${vh}px`);

            // Trigger resize event for other components
            window.dispatchEvent(new Event('resize'));
        }, 100);
    }

    // Listen for orientation changes
    window.addEventListener('orientationchange', handleOrientationChange);

    // Initial setup
    handleOrientationChange();
}

/**
 * Accessibility enhancements for mobile
 */
function initAccessibilityEnhancements() {
    // Add skip links for mobile navigation
    const skipLink = document.createElement('a');
    skipLink.href = '#main-content';
    skipLink.textContent = 'Saltar al contenido principal';
    skipLink.className = 'skip-link';
    skipLink.style.cssText = `
        position: absolute;
        top: -40px;
        left: 6px;
        background: #000;
        color: #fff;
        padding: 8px;
        text-decoration: none;
        border-radius: 4px;
        z-index: 1000;
        transition: top 0.3s;
    `;

    skipLink.addEventListener('focus', function() {
        this.style.top = '6px';
    });

    skipLink.addEventListener('blur', function() {
        this.style.top = '-40px';
    });

    document.body.insertBefore(skipLink, document.body.firstChild);

    // Add main content ID if not present
    const content = document.querySelector('.content');
    if (content && !content.id) {
        content.id = 'main-content';
    }

    // Improve focus management for mobile
    const focusableElements = document.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');

    focusableElements.forEach(element => {
        element.addEventListener('focus', function() {
            // Ensure focused element is visible on mobile
            if (window.innerWidth <= 767) {
                setTimeout(() => {
                    this.scrollIntoView({
                        behavior: 'smooth',
                        block: 'nearest'
                    });
                }, 100);
            }
        });
    });
}

/**
 * Copy color to clipboard with fallback
 */
function copyColorToClipboard(color) {
    const textToCopy = color.startsWith('#') ? color : '#' + color;

    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(textToCopy).catch(err => {
            fallbackCopyTextToClipboard(textToCopy);
        });
    } else {
        fallbackCopyTextToClipboard(textToCopy);
    }
}

/**
 * Fallback copy method for older browsers
 */
function fallbackCopyTextToClipboard(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-999999px';
    textArea.style.top = '-999999px';
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        document.execCommand('copy');
    } catch (err) {
        console.error('Fallback: Oops, unable to copy', err);
    }

    document.body.removeChild(textArea);
}

/**
 * Show mobile-optimized toast notification
 */
function showMobileToast(message, duration = 3000) {
    // Remove existing toast
    const existingToast = document.querySelector('.mobile-toast');
    if (existingToast) {
        existingToast.remove();
    }

    // Create new toast
    const toast = document.createElement('div');
    toast.className = 'mobile-toast';
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%) translateY(-100px);
        background: #28a745;
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
        z-index: 1000;
        max-width: 90%;
        text-align: center;
        font-size: 14px;
        transition: transform 0.3s ease;
    `;

    document.body.appendChild(toast);

    // Show toast
    setTimeout(() => {
        toast.style.transform = 'translateX(-50%) translateY(0)';
    }, 100);

    // Hide toast
    setTimeout(() => {
        toast.style.transform = 'translateX(-50%) translateY(-100px)';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }, duration);
}

/**
 * Add CSS for touch feedback
 */
function addTouchFeedbackStyles() {
    const style = document.createElement('style');
    style.textContent = `
        .touch-active {
            transform: scale(0.95) !important;
            opacity: 0.8 !important;
            transition: all 0.1s ease !important;
        }

        @media (max-width: 767px) {
            /* Improve touch targets */
            .btn, .remove-color-btn {
                min-height: 28px;
                min-width: 44px;
            }

            /* Better spacing for touch */
            .combination-actions .btn {
                margin-bottom: 4px;
                padding: 2px 8px;
                font-size: 12px;
                line-height: 1.1;
            }

            /* Improve form controls */
            .form-control {
                min-height: 44px;
            }

            /* Better color field spacing */
            .color-field {
                padding: 20px 15px;
                margin-bottom: 20px;
            }
        }

        @media (max-width: 480px) {
            /* Very small screens */
            .btn {
                font-size: 12px;
                padding: 2px 8px;
            }

            .combination-actions .btn {
                padding: 2px 6px;
                font-size: 11px;
            }

            .color-preview {
                width: 50px;
                height: 50px;
            }
        }
    `;

    document.head.appendChild(style);
}

// Add touch feedback styles
addTouchFeedbackStyles();
